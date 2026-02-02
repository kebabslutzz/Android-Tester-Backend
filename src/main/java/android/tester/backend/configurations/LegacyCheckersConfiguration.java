package android.tester.backend.configurations;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments.UnlocalizedIconsCheck;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments.UnreadableTextCheck;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments.WrongEncodingCheck;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments.WrongLanguageCheck;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LegacyCheckersConfiguration {
  @Bean
  public List<Object> registeredCheckers() {
    return List.of(
      new UnreadableTextCheck(),
      new WrongEncodingCheck(),
      new UnlocalizedIconsCheck(),
      new WrongLanguageCheck()
      // Add other legacy checkers here
    );
  }
}
