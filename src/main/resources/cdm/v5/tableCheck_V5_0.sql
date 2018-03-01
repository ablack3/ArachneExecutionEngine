SELECT attribute_definition_id, attribute_name, attribute_description, attribute_type_concept_id, attribute_syntax
FROM @cdm_schema.attribute_definition;
SELECT care_site_id, care_site_name, place_of_service_concept_id, location_id, care_site_source_value, place_of_service_source_value
FROM @cdm_schema.care_site;
SELECT cdm_source_name, cdm_source_abbreviation, cdm_holder, source_description, source_documentation_reference, cdm_etl_reference, source_release_date, cdm_release_date, cdm_version, vocabulary_version
FROM @cdm_schema.cdm_source;
SELECT cohort_definition_id, subject_id, cohort_start_date, cohort_end_date
FROM @cdm_schema.cohort;
SELECT cohort_definition_id, cohort_start_date, cohort_end_date, subject_id, attribute_definition_id, value_as_number, value_as_concept_id
FROM @cdm_schema.cohort_attribute;
SELECT cohort_definition_id, cohort_definition_name, cohort_definition_description, definition_type_concept_id, cohort_definition_syntax, subject_concept_id, cohort_initiation_date
FROM @cdm_schema.cohort_definition;
SELECT concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.concept;
SELECT ancestor_concept_id, descendant_concept_id, min_levels_of_separation, max_levels_of_separation
FROM @cdm_schema.concept_ancestor;
SELECT concept_class_id, concept_class_name, concept_class_concept_id
FROM @cdm_schema.concept_class;
SELECT concept_id_1, concept_id_2, relationship_id, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.concept_relationship;
SELECT concept_id, concept_synonym_name, language_concept_id
FROM @cdm_schema.concept_synonym;
SELECT condition_era_id, person_id, condition_concept_id, condition_era_start_date, condition_era_end_date, condition_occurrence_count
FROM @cdm_schema.condition_era;
SELECT condition_occurrence_id, person_id, condition_concept_id, condition_start_date, condition_end_date, condition_type_concept_id, stop_reason, provider_id, visit_occurrence_id, condition_source_value, condition_source_concept_id
FROM @cdm_schema.condition_occurrence;
SELECT person_id, death_date, death_type_concept_id, cause_concept_id, cause_source_value, cause_source_concept_id
FROM @cdm_schema.death;
SELECT device_cost_id, device_exposure_id, currency_concept_id, paid_copay, paid_coinsurance, paid_toward_deductible, paid_by_payer, paid_by_coordination_benefits, total_out_of_pocket, total_paid, payer_plan_period_id
FROM @cdm_schema.device_cost;
SELECT device_exposure_id, person_id, device_concept_id, device_exposure_start_date, device_exposure_end_date, device_type_concept_id, unique_device_id, quantity, provider_id, visit_occurrence_id, device_source_value, device_source_concept_id
FROM @cdm_schema.device_exposure;
SELECT domain_id, domain_name, domain_concept_id
FROM @cdm_schema."domain";
SELECT dose_era_id, person_id, drug_concept_id, unit_concept_id, dose_value, dose_era_start_date, dose_era_end_date
FROM @cdm_schema.dose_era;
SELECT drug_cost_id, drug_exposure_id, currency_concept_id, paid_copay, paid_coinsurance, paid_toward_deductible, paid_by_payer, paid_by_coordination_benefits, total_out_of_pocket, total_paid, ingredient_cost, dispensing_fee, average_wholesale_price, payer_plan_period_id
FROM @cdm_schema.drug_cost;
SELECT drug_era_id, person_id, drug_concept_id, drug_era_start_date, drug_era_end_date, drug_exposure_count, gap_days
FROM @cdm_schema.drug_era;
SELECT drug_exposure_id, person_id, drug_concept_id, drug_exposure_start_date, drug_exposure_end_date, drug_type_concept_id, stop_reason, refills, quantity, days_supply, sig, route_concept_id, effective_drug_dose, dose_unit_concept_id, lot_number, provider_id, visit_occurrence_id, drug_source_value, drug_source_concept_id, route_source_value, dose_unit_source_value
FROM @cdm_schema.drug_exposure;
SELECT drug_concept_id, ingredient_concept_id, amount_value, amount_unit_concept_id, numerator_value, numerator_unit_concept_id, denominator_unit_concept_id, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.drug_strength;
SELECT domain_concept_id_1, fact_id_1, domain_concept_id_2, fact_id_2, relationship_concept_id
FROM @cdm_schema.fact_relationship;
SELECT location_id, address_1, address_2, city, state, zip, county, location_source_value
FROM @cdm_schema.location;
SELECT measurement_id, person_id, measurement_concept_id, measurement_date, measurement_time, measurement_type_concept_id, operator_concept_id, value_as_number, value_as_concept_id, unit_concept_id, range_low, range_high, provider_id, visit_occurrence_id, measurement_source_value, measurement_source_concept_id, unit_source_value, value_source_value
FROM @cdm_schema.measurement;
SELECT note_id, person_id, note_date, note_time, note_type_concept_id, note_text, provider_id, visit_occurrence_id, note_source_value
FROM @cdm_schema.note;
SELECT observation_id, person_id, observation_concept_id, observation_date, observation_time, observation_type_concept_id, value_as_number, value_as_string, value_as_concept_id, qualifier_concept_id, unit_concept_id, provider_id, visit_occurrence_id, observation_source_value, observation_source_concept_id, unit_source_value, qualifier_source_value
FROM @cdm_schema.observation;
SELECT observation_period_id, person_id, observation_period_start_date, observation_period_end_date, period_type_concept_id
FROM @cdm_schema.observation_period;
SELECT payer_plan_period_id, person_id, payer_plan_period_start_date, payer_plan_period_end_date, payer_source_value, plan_source_value, family_source_value
FROM @cdm_schema.payer_plan_period;
SELECT person_id, gender_concept_id, year_of_birth, month_of_birth, day_of_birth, time_of_birth, race_concept_id, ethnicity_concept_id, location_id, provider_id, care_site_id, person_source_value, gender_source_value, gender_source_concept_id, race_source_value, race_source_concept_id, ethnicity_source_value, ethnicity_source_concept_id
FROM @cdm_schema.person;
SELECT procedure_cost_id, procedure_occurrence_id, currency_concept_id, paid_copay, paid_coinsurance, paid_toward_deductible, paid_by_payer, paid_by_coordination_benefits, total_out_of_pocket, total_paid, revenue_code_concept_id, payer_plan_period_id, revenue_code_source_value
FROM @cdm_schema.procedure_cost;
SELECT procedure_occurrence_id, person_id, procedure_concept_id, procedure_date, procedure_type_concept_id, modifier_concept_id, quantity, provider_id, visit_occurrence_id, procedure_source_value, procedure_source_concept_id, qualifier_source_value
FROM @cdm_schema.procedure_occurrence;
SELECT provider_id, provider_name, npi, dea, specialty_concept_id, care_site_id, year_of_birth, gender_concept_id, provider_source_value, specialty_source_value, specialty_source_concept_id, gender_source_value, gender_source_concept_id
FROM @cdm_schema.provider;
SELECT relationship_id, relationship_name, is_hierarchical, defines_ancestry, reverse_relationship_id, relationship_concept_id
FROM @cdm_schema.relationship;
SELECT source_code, source_concept_id, source_vocabulary_id, source_code_description, target_concept_id, target_vocabulary_id, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.source_to_concept_map;
SELECT specimen_id, person_id, specimen_concept_id, specimen_type_concept_id, specimen_date, specimen_time, quantity, unit_concept_id, anatomic_site_concept_id, disease_status_concept_id, specimen_source_id, specimen_source_value, unit_source_value, anatomic_site_source_value, disease_status_source_value
FROM @cdm_schema.specimen;
SELECT visit_cost_id, visit_occurrence_id, currency_concept_id, paid_copay, paid_coinsurance, paid_toward_deductible, paid_by_payer, paid_by_coordination_benefits, total_out_of_pocket, total_paid, payer_plan_period_id
FROM @cdm_schema.visit_cost;
SELECT visit_occurrence_id, person_id, visit_concept_id, visit_start_date, visit_start_time, visit_end_date, visit_end_time, visit_type_concept_id, provider_id, care_site_id, visit_source_value, visit_source_concept_id
FROM @cdm_schema.visit_occurrence;
SELECT vocabulary_id, vocabulary_name, vocabulary_reference, vocabulary_version, vocabulary_concept_id
FROM @cdm_schema.vocabulary;