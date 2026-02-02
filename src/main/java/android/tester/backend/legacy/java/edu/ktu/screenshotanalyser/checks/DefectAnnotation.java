package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import org.opencv.core.Rect;

public record DefectAnnotation(IStateRuleChecker defect, Rect bounds, String message) {
}
