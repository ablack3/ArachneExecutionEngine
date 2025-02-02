/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.execution.r;

import static org.apache.commons.io.IOUtils.closeQuietly;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TarballRService extends RService implements ExecutionService {
    private static final String EXECUTION_COMMAND = "Rscript";

    private static final String RUNTIME_ANALYSIS_ID = "ANALYSIS_ID";

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RIsolatedRuntimeProperties rIsolatedRuntimeProps;

    @PostConstruct
    public void init() {
        if (RuntimeServiceMode.ISOLATED.equals(getRuntimeServiceMode())) {
            log.info("Runtime service running in ISOLATED environment mode");
        } else {
            log.info("Runtime service running in SINGLE mode");
        }
    }

    @Override
    @FileDescriptorCount
    public Overseer analyze(
            AnalysisSyncRequestDTO analysis, File file, DescriptorBundle descriptorBundle,
            KrbConfig krbConfig, BiConsumer<String, String> callback, Integer updateInterval
    ) {
        Long id = analysis.getId();
        String executableFileName = analysis.getExecutableFileName();
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();

        try {
            Instant started = Instant.now();
            Map<String, String> envp = buildRuntimeEnvVariables(dataSource, krbConfig.getIsolatedRuntimeEnvs());
            File jailFile = new File(rIsolatedRuntimeProps.getJailSh());
            boolean externalJail = jailFile.isFile();
            File runFile = externalJail ? jailFile : extractToTempFile(resourceLoader, "classpath:/jail.sh", "ee", ".sh");
            log.info("Execution [{}] initializing jail [{}]", id, runFile.getAbsolutePath());
            prepareEnvironmentInfoFile(file, descriptorBundle.getDescriptor());
            prepareRprofile(file);
            envp.put(RUNTIME_ANALYSIS_ID, id.toString());
            String[] command = buildRuntimeCommand(runFile, file, executableFileName, descriptorBundle.getPath());
            ProcessBuilder pb = new ProcessBuilder(command).directory(file).redirectErrorStream(true);
            pb.environment().putAll(envp);
            log.info("Execution [{}] start R process: {}", id, command);
            Process process = pb.start();
            String descriptorId = descriptorBundle.getDescriptor().getId();
            return new TarballROverseer(
                    id, process, runtimeTimeOutSec, callback, updateInterval, started, descriptorId, killTimeoutSec
            ).whenComplete((outcome, throwable) -> {
                if (!externalJail) {
                    FileUtils.deleteQuietly(runFile);
                }
                FileUtils.deleteQuietly(krbConfig.getComponents().getKeytabPath().toFile());
                if (RuntimeServiceMode.ISOLATED == getRuntimeServiceMode()) {
                    FileUtils.deleteQuietly(krbConfig.getConfPath().toFile());
                }
                cleanupEnv(file, outcome);
            });

        } catch (IOException ex) {
            log.error("Execution [{}] error building runtime command", id, ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }


    private void prepareEnvironmentInfoFile(File workDir, Descriptor descriptor) {
        final String lineDelimiter = StringUtils.repeat("-", 32);
        try (FileWriter fw = new FileWriter(new File(workDir, "environment.txt")); PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("Analysis Runtime Environment is %s(%s):[%s]\n", descriptor.getBundleName(), descriptor.getLabel(), descriptor.getId());
            if (descriptor.getOsLibraries() != null) {
                pw.println(lineDelimiter);
                pw.println("Operating System Libraries:");
                pw.println(lineDelimiter);
                descriptor.getOsLibraries().forEach(pw::println);
            }
            for (ExecutionRuntime runtime : descriptor.getExecutionRuntimes()) {
                if (runtime instanceof RExecutionRuntime) {
                    pw.println(lineDelimiter);
                    pw.println("R Execution Runtime Libraries:");
                    pw.println(lineDelimiter);
                    RExecutionRuntime rRuntime = (RExecutionRuntime) runtime;
                    for (RDependency rDependency : rRuntime.getDependencies()) {
                        pw.println(rDependency.getName() + " " + rDependency.getVersion() + " " + rDependency.getOwner() + " " + rDependency.getDependencySourceType());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to write environment info file", e);
        }
    }

    private void prepareRprofile(File workDir) throws IOException {
        try (InputStream is = resourceLoader.getResource("classpath:/Rprofile").getInputStream()) {
            FileUtils.copyToFile(is, new File(workDir, ".Rprofile"));
        }
    }

    private ExecutionOutcome cleanupEnv(File directory, ExecutionOutcome outcome) {
        try {
            File cleanupScript = new File(rIsolatedRuntimeProps.getCleanupSh());
            boolean isExternal = true;

            if (!cleanupScript.exists()) {
                cleanupScript = extractToTempFile(resourceLoader, "classpath:/cleanup.sh", "ee", ".sh");
                isExternal = false;
            }
            Process p = null;
            try {
                ProcessBuilder pb = new ProcessBuilder((String[]) ArrayUtils.addAll(rIsolatedRuntimeProps.getRunCmd(), new String[]{cleanupScript.getAbsolutePath(), directory.getAbsolutePath()}));
                p = pb.start();
                p.waitFor();
            } catch (InterruptedException ignored) {
            } finally {
                if (!isExternal) {
                    FileUtils.deleteQuietly(cleanupScript);
                }
                if (Objects.nonNull(p)) {
                    closeQuietly(p.getOutputStream());
                    closeQuietly(p.getInputStream());
                    closeQuietly(p.getErrorStream());
                }
            }
            return outcome;
        } catch (IOException e) {
            log.error("Error cleaning up environment [{}]", directory.getPath(), e);
            return outcome.addError("Error cleaning up environment: " + e.getMessage());
        }
    }

    private String[] buildRuntimeCommand(File runFile, File workingDir, String fileName, String bundlePath)
            throws FileNotFoundException {

        if (!workingDir.exists()) {
            throw new FileNotFoundException("Working directory with name" + workingDir.getAbsolutePath() + "is not exists");
        }
        File file = Paths.get(workingDir.getAbsolutePath(), fileName).toFile();
        if (file.isDirectory()) {
            throw new FileNotFoundException("file '" + fileName + "' must be a regular file");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("file '"
                    + fileName + "' is not exists in directory '" + workingDir.getAbsolutePath() + "'");
        }
        String[] command;
        if (RuntimeServiceMode.ISOLATED.equals(getRuntimeServiceMode())) {
            command = ArrayUtils.addAll(rIsolatedRuntimeProps.getRunCmd(),
                    runFile.getAbsolutePath(), workingDir.getAbsolutePath(), fileName, bundlePath);
        } else {
            command = new String[]{EXECUTION_COMMAND, fileName};
        }
        return command;
    }

    private RuntimeServiceMode getRuntimeServiceMode() {
        return StringUtils.isNotBlank(rIsolatedRuntimeProps.getArchive()) ? RuntimeServiceMode.ISOLATED : RuntimeServiceMode.SINGLE;
    }


    @SuppressWarnings("SameParameterValue")
    private static File extractToTempFile(ResourceLoader loader, String resourceName, String prefix, String suffix) throws IOException {
        File runFile = Files.createTempFile(prefix, suffix).toFile();
        try (InputStream in = loader.getResource(resourceName).getInputStream()) {
            FileUtils.copyToFile(in, runFile);
        }
        return runFile;
    }

}
