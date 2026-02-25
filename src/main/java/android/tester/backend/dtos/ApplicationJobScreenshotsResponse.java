package android.tester.backend.dtos;

import java.util.List;
import java.util.UUID;

public record ApplicationJobScreenshotsResponse(
  UUID applicationId,
  String applicationName,
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
