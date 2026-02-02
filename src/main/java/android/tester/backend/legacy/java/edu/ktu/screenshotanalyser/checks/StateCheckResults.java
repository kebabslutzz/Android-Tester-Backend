package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;

import java.util.ArrayList;
import java.util.List;

public class StateCheckResults {
  public StateCheckResults(State state) {
    this.state = state;
  }

  public State getState() {
    return this.state;
  }

  public List<DefectAnnotation> getAnnotations() {
    return this.annotations;
  }

  public void addAnnotation(DefectAnnotation annotation) {
    this.annotations.add(annotation);
  }

  private final State state;
  private final List<DefectAnnotation> annotations = new ArrayList<>();
}
