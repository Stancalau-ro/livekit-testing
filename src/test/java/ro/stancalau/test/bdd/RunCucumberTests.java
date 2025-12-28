package ro.stancalau.test.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * General Cucumber test runner for all BDD tests.
 *
 * <p>Usage in IntelliJ: 1. Right-click on this class and select "Run 'RunCucumberTests'" 2. Or
 * create a JUnit run configuration targeting this class
 *
 * <p>This runs all feature files in the features directory.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "ro.stancalau.test.bdd.steps")
@Execution(ExecutionMode.CONCURRENT)
public class RunCucumberTests {}
