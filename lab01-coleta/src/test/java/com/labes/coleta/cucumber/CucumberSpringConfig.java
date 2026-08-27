package com.labes.coleta.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

@CucumberContextConfiguration
@SpringBootTest
@TestExecutionListeners(listeners = {}, mergeMode = MergeMode.REPLACE_DEFAULTS)
public class CucumberSpringConfig {
}
