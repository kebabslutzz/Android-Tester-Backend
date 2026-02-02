package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.utils.BaseLogger;

import java.sql.SQLException;

public class DataBaseResultsCollector implements IResultsCollector {
  public DataBaseResultsCollector(String name, boolean resume) {
    this.testsRunId = this.dataBase.startTestsRun(name, resume);
  }


  /*


  @Override
  public boolean wasChecked(State state)
  {
    var fileName = state.getImageFile().getAbsolutePath();

    if (fileName.startsWith(Settings.appImagesFolder.getAbsolutePath()))
    {
      fileName = fileName.substring(Settings.appImagesFolder.getAbsolutePath().length() + 1);
    }

    synchronized (this)
    {
      if (null == this.checkedStates)
      {
        this.checkedStates = new HashSet<String>(this.statisticsManager.getCheckedStates(this.testsRunId));
      }
    }

    return this.checkedStates.contains(fileName);


    //return this.statisticsManager.wasChecked(this.testsRunId, state);
  }
*/
  @Override
  public void finishRun() {
    try {
      this.dataBase.finishRun(this.testsRunId);
    } catch (SQLException ex) {
      BaseLogger.logException("", ex);
    }
  }

  @Override
  public void finishedState(State state, StateCheckResults results) {
    try {
      this.dataBase.update("UPDATE TestRun SET ScreenShotsAnalyzed = ScreenShotsAnalyzed + 1 WHERE Id = ?", this.testsRunId);
      this.dataBase.logDetectedDefects(this.testsRunId, results);
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  private final long testsRunId;
  private final DataBase dataBase = new DataBase();
//	private HashSet<String> checkedStates = null;
}
