package android.tester.backend.dtos.screenshots;

import java.util.List;
import java.util.UUID;

public record JobScreenshotDTO(
  UUID applicationId,
  String appName,
  List<JobGroupDTO> jobs
) {
  public record JobGroupDTO(
    UUID jobId,
    UUID testRunId,
    List<ScreenshotMetadataDTO> screenshots
  ) {
  }

  public record ScreenshotMetadataDTO(
    UUID screenshotId,
    String fileName,
    String downloadUrl
  ) {
  }
}
