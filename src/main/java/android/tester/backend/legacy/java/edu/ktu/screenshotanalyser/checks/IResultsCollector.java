package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;

/**
 * Collects all analysis results in a thread safe manner.
 */
public interface IResultsCollector {
  void finishRun();

  void finishedState(State state, StateCheckResults results);
}
