package android.tester.backend.services.logging;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class LogService {
  private static final int MAX_LOG_SIZE = 1000;
  private final ConcurrentLinkedDeque<String> logBuffer = new ConcurrentLinkedDeque<>();

  public void addLog(String message) {
    String entry = "[CMD] " + message;

    logBuffer.add(entry);

    while (logBuffer.size() > MAX_LOG_SIZE) {
      logBuffer.poll();
    }

    System.out.println(entry);
  }

  public List<String> getLogs() {
    return new ArrayList<>(logBuffer);
  }

  public void clearLogs() {
    logBuffer.clear();
  }
}
