package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.tools;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase;

import java.util.ArrayList;
import java.util.List;

public class StatisticsManager extends DataBase {
  public boolean wasChecked(long testRunId, State state) {
    try (var connection = beginTransaction()) {
      var screenshotId = getScreenShotId(state, connection);

      var logId = getId(connection, "SELECT TOP 1 Id FROM TestRunDefect WHERE TestRunId = ? AND ScreenShotId = ? AND DefectTypeId = 34", testRunId, screenshotId);

      return null != logId;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return false;
  }

  public List<String> getCheckedStates(long testRunId) {
    try {
      var query = "SELECT DISTINCT ScreenShot.FileName FROM TestRunDefect JOIN ScreenShot ON TestRunDefect.ScreenShotId = ScreenShot.Id AND TestRunDefect.TestRunId = ?";
      return getList(rs -> rs.getString(1), query, testRunId);
    } catch (Exception ex) {
      ex.printStackTrace();

      return new ArrayList<>();
    }
  }
}
