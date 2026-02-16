package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.BaseRuleCheck;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.DefectAnnotation;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.IStateRuleChecker;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.StateCheckResults;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.Control;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;

public class UnreadableTextCheck extends BaseRuleCheck implements IStateRuleChecker {
  /**
   * Minimum readable text size in millimeters.
   */
  private static final double MIN_HEIGHT = 2.0;

  public UnreadableTextCheck() {
    super(19, "TS2");
  }

  @Override
  public void analyze(State state, StateCheckResults results) {
    // If device information is missing, we cannot calculate physical size
    if (state.getTestDevice() == null) {
      return;
    }

    for (var control : state.getActualControls()) {
      var result = isTextTooSmall(control, state);

      if (null != result) {
        results.addAnnotation(new DefectAnnotation(this, control.getBounds(), result));

        return;
      }
    }
  }

  private String isTextTooSmall(Control message, State state) {
    if ((message.getText() == null) || (message.getText().trim().isEmpty())) {
      return null;
    }

    if ((message.getBounds().height <= 3) || (message.getBounds().width <= 3)) {
      return null;
    }

    var actualHeight = state.getTestDevice().getPhysicalSize(message.getBounds().height);

    if (actualHeight < MIN_HEIGHT) {
      if (message.getBounds().y + message.getBounds().height >= state.getImageSize().height) {
        // skip screen edge
      } else {
        if ((null != message.getParent()) && (message.getBounds().y + message.getBounds().height >= message.getParent().getBounds().y + message.getParent().getBounds().height)) {
          // skip parent edge
        } else {
          return "Text : [" + message.getText() + "] too small " + actualHeight + "mm " + message.getBounds().toString();
        }
      }
    }

    return null;
  }
}
