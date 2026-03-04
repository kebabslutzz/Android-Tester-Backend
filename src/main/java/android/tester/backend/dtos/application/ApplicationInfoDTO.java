package android.tester.backend.dtos.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationInfoDTO(
  UUID applicationId,
  String applicationName,
  String packageName,
  String versioName,
  OffsetDateTime createdAt,
  List<JobGroupDTO> jobs
) {
  public record JobGroupDTO(
    UUID jobId,
    UUID testRunId,
    OffsetDateTime createdAt,
    String status
  ) {
  }
}
