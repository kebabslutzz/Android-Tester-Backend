package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application;

import java.util.ArrayList;
import java.util.List;

public class AppCheckResults {
  public AppCheckResults(Application application) {
    this.application = application;
  }

  public Application getApplication() {
    return this.application;
  }

  public List<AppDefectAnnotation> getAnnotations() {
    return this.annotations;
  }

  public void addAnnotation(AppDefectAnnotation annotation) {
    this.annotations.add(annotation);
  }

  private final Application application;
  private final List<AppDefectAnnotation> annotations = new ArrayList<>();
}
