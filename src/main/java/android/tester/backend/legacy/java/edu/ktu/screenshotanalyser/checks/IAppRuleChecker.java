package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.AppContext;

import javax.annotation.Nonnull;

/**
 * Checks all application for defects in one go.
 */
public interface IAppRuleChecker {
  void analyze(@Nonnull AppContext appContext, @Nonnull AppCheckResults results);
}
