package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;

import javax.annotation.Nonnull;

/**
 * Checks one application's screenshot for defects.
 */
public interface IStateRuleChecker {
  /**
   * Analyzes sate and adds defects to result.
   *
   * @param state  state to analyze.
   * @param result results collector
   */
  void analyze(@Nonnull State state, @Nonnull StateCheckResults result);

  /**
   * @return defects identifier.
   */
  long getId();
}
