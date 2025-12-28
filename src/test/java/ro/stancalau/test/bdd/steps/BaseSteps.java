package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;

/**
 * Base step definition class that handles manager lifecycle for all scenarios. Provides centralized
 * manager initialization and cleanup. Other step definition classes should use ManagerProvider for
 * clean access.
 */
@Slf4j
public class BaseSteps {

  @Before(order = 0)
  public void setUpManagers(Scenario scenario) {
    ManagerProvider.initializeManagers();
  }

  @After(order = 1000)
  public void cleanupManagers(Scenario scenario) {
    ManagerProvider.cleanupManagers();
  }
}
