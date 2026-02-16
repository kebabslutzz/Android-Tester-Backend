package android.tester.backend.dtos;

import java.util.UUID;

public record StartTestRequest(
  UUID appId,
  String deviceName,
  String resolution,
  Long screenSize,
  Boolean tablet,
  String osVersion
) {
}
