package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.util.BigQueryUtils;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.config.properties.HiveBulkLoadProperties;
import com.odysseusinc.arachne.executionengine.execution.DriverLocations;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.KerberosSupport;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public abstract class RService implements ExecutionService {
    private static final String RUNTIME_ENV_DATA_SOURCE_NAME = "DATA_SOURCE_NAME";
    private static final String RUNTIME_ENV_DBMS_USERNAME = "DBMS_USERNAME";
    private static final String RUNTIME_ENV_DBMS_PASSWORD = "DBMS_PASSWORD";
    private static final String RUNTIME_ENV_DBMS_TYPE = "DBMS_TYPE";
    private static final String RUNTIME_ENV_CONNECTION_STRING = "CONNECTION_STRING";
    private static final String RUNTIME_ENV_DBMS_SCHEMA = "DBMS_SCHEMA";
    private static final String RUNTIME_ENV_TARGET_SCHEMA = "TARGET_SCHEMA";
    private static final String RUNTIME_ENV_RESULT_SCHEMA = "RESULT_SCHEMA";
    private static final String RUNTIME_ENV_COHORT_TARGET_TABLE = "COHORT_TARGET_TABLE";
    private static final String RUNTIME_ENV_PATH_KEY = "PATH";
    private static final String RUNTIME_ENV_PATH_VALUE = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";
    private static final String RUNTIME_ENV_HOME_KEY = "HOME";
    private static final String RUNTIME_ENV_HOME_VALUE = "/root";
    private static final String RUNTIME_ENV_HOSTNAME_KEY = "HOSTNAME";
    private static final String RUNTIME_ENV_HOSTNAME_VALUE = "moby";
    private static final String RUNTIME_ENV_LANG_KEY = "LANG";
    private static final String RUNTIME_ENV_LANG_VALUE = "en_US.UTF-8";
    private static final String RUNTIME_ENV_LC_ALL_KEY = "LC_ALL";
    private static final String RUNTIME_ENV_LC_ALL_VALUE = "en_US.UTF-8";
    private static final String RUNTIME_ENV_DRIVER_PATH = "JDBC_DRIVER_PATH";
    private static final String RUNTIME_BQ_KEYFILE = "BQ_KEYFILE";
    @Value("${runtime.killTimeoutSec:30}")
    protected int killTimeoutSec;
    @Value("${runtime.timeOutSec}")
    protected int runtimeTimeOutSec;

    @Autowired
    private DescriptorService descriptorService;
    @Autowired
    private KerberosSupport kerberosSupport;
    @Value("${bulkload.enableMPP}")
    private boolean enableMPP;
    @Autowired
    private HiveBulkLoadProperties hiveBulkLoadProperties;
    @Autowired
    private DriverLocations drivers;

    private static String sanitizeFilename(String filename) {
        return Objects.requireNonNull(filename).replaceAll("[<>:\"/\\\\|?*\\u0000]", "");
    }

    public String getExtension() {
        return "r";
    }

    public Overseer analyze(AnalysisSyncRequestDTO analysis, File analysisDir, BiConsumer<String, String> callback, Integer updateInterval) {
        File keystoreDir = new File(analysisDir, "keys");
        KrbConfig krbConfig = kerberosSupport.getConfig(analysis, keystoreDir);

        DescriptorBundle bundle = descriptorService.getDescriptorBundle(analysisDir,
                analysis.getId(), analysis.getRequestedDescriptorId()
        );
        Overseer overseer = analyze(
                analysis, analysisDir, bundle, krbConfig, callback, updateInterval
        ).whenComplete((outcome, throwable) -> {
            // Keystore folder must be deleted before zipping results
            FileUtils.deleteQuietly(keystoreDir);
        });

        String actualDescriptorId = bundle.getDescriptor().getId();
        log.info("Execution [{}] with actual descriptor='{}' started in R Runtime Service", analysis.getId(), actualDescriptorId);
        return overseer;
    }

    @FileDescriptorCount
    public abstract Overseer analyze(
            AnalysisSyncRequestDTO analysis, File file, DescriptorBundle descriptorBundle,
            KrbConfig krbConfig, BiConsumer<String, String> callback, Integer updateInterval
    );

    protected Map<String, String> buildRuntimeEnvVariables(DataSourceUnsecuredDTO dataSource, Map<String, String> krbProps) {

        Map<String, String> environment = new HashMap<>(krbProps);
        environment.put(RUNTIME_ENV_DATA_SOURCE_NAME, RService.sanitizeFilename(dataSource.getName()));
        environment.put(RUNTIME_ENV_DBMS_USERNAME, dataSource.getUsername());
        environment.put(RUNTIME_ENV_DBMS_PASSWORD, dataSource.getPassword());
        environment.put(RUNTIME_ENV_DBMS_TYPE, dataSource.getType().getOhdsiDB());
        environment.put(RUNTIME_ENV_CONNECTION_STRING, dataSource.getConnectionString());
        environment.put(RUNTIME_ENV_DBMS_SCHEMA, dataSource.getCdmSchema());
        environment.put(RUNTIME_ENV_TARGET_SCHEMA, dataSource.getTargetSchema());
        environment.put(RUNTIME_ENV_RESULT_SCHEMA, dataSource.getResultSchema());
        environment.put(RUNTIME_ENV_COHORT_TARGET_TABLE, dataSource.getCohortTargetTable());
        environment.put(RUNTIME_ENV_DRIVER_PATH, drivers.getPath(dataSource.getType()));
        environment.put(RUNTIME_BQ_KEYFILE, getBigQueryKeyFile(dataSource));
        environment.put(RUNTIME_ENV_PATH_KEY, RUNTIME_ENV_PATH_VALUE);
        environment.put(RUNTIME_ENV_HOME_KEY, getUserHome());
        environment.put(RUNTIME_ENV_HOSTNAME_KEY, RUNTIME_ENV_HOSTNAME_VALUE);
        environment.put(RUNTIME_ENV_LANG_KEY, RUNTIME_ENV_LANG_VALUE);
        environment.put(RUNTIME_ENV_LC_ALL_KEY, RUNTIME_ENV_LC_ALL_VALUE);

        if (enableMPP) {
            exposeMPPEnvironmentVariables(environment);
        }

        environment.values().removeIf(Objects::isNull);
        return environment;
    }

    private void exposeMPPEnvironmentVariables(Map<String, String> environment) {

        environment.put("USE_MPP_BULK_LOAD", Boolean.toString(enableMPP));
        environment.put("HIVE_NODE_HOST", hiveBulkLoadProperties.getHost());
        environment.put("HIVE_SSH_USER", hiveBulkLoadProperties.getSsh().getUsername());
        environment.put("HIVE_SSH_PORT", Integer.toString(hiveBulkLoadProperties.getSsh().getPort()));
        environment.put("HIVE_SSH_PASSWORD", hiveBulkLoadProperties.getSsh().getPassword());
        if (StringUtils.isNotBlank(hiveBulkLoadProperties.getSsh().getKeyfile())) {
            environment.put("HIVE_KEYFILE", hiveBulkLoadProperties.getSsh().getKeyfile());
        }
        if (StringUtils.isNotBlank(hiveBulkLoadProperties.getHadoop().getUsername())) {
            environment.put("HADOOP_USER_NAME", hiveBulkLoadProperties.getHadoop().getUsername());
        }
        environment.put("HIVE_NODE_PORT", Integer.toString(hiveBulkLoadProperties.getHadoop().getPort()));
    }

    private String getUserHome() {
        String userHome = System.getProperty("user.home");
        return StringUtils.defaultString(userHome, RUNTIME_ENV_HOME_VALUE);
    }

    private String getBigQueryKeyFile(DataSourceUnsecuredDTO dataSource) {

        return dataSource.getType().equals(DBMSType.BIGQUERY) ?
                BigQueryUtils.getBigQueryKeyPath(dataSource.getConnectionString()) : null;
    }
}
