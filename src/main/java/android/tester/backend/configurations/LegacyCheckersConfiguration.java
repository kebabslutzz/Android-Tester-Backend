package android.tester.backend.configurations;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.IStateRuleChecker;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LegacyCheckersConfiguration {
  @Bean
  public List<IStateRuleChecker> registeredCheckers() {
    return List.of(
      new UnreadableTextCheck(),
      new WrongEncodingCheck(),
      new UnlocalizedIconsCheck(),
      new WrongLanguageCheck(),
//      new BadScalingCheck(),
      new BlurredImagesCheck(),
      new ClashingBackgroundCheck(),
      new ClippedControlCheck(),
      new ClippedTextCheck(),
      new GrammarCheck(),
      new MissingTextCheck(),
      new MissingTranslationCheck(),
//      new MixedLanguagesStateCheck(),
      new ObscuredControlCheck(),
      new ObscuredTextCheck(),
//      new OffensiveMessagesCheck(),
      new TooHardToUnderstandCheck()
//      new UnalignedControlsCheck()
    );
  }
}
