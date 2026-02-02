package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.*;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.AppContext;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.Control;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import org.languagetool.Language;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class GrammarCheck extends BaseTextRuleCheck implements IStateRuleChecker, IAppRuleChecker {
  public GrammarCheck() {
    super(2, "BadSpelling");
  }

  @Override
  public void analyze(@Nonnull State state, @Nonnull StateCheckResults result) {
    var allTexts = state.getActualControls().stream().map(this::getText).collect(Collectors.joining(". "));
    var candidateLanguages = determineLanguageAll(allTexts);

    if (candidateLanguages.isEmpty()) {
      return;
    }

    for (var language : candidateLanguages) {
      if (language.equals("lt")) {
        return;
      }
    }

    var languages = new ArrayList<Language>();
    candidateLanguages.forEach(p -> languages.addAll(getLanguageByCode(p)));

    if (languages.isEmpty()) {
      return;
    }

    for (var control : state.getActualControls()) {
      if (checkControlTexts(state, result, languages, control)) {
        return;
      }
    }
  }

  private boolean checkControlTexts(State state, StateCheckResults result, ArrayList<Language> languages, Control control) {
    if (null != control.getText() && control.getText().trim().length() > 0) {
      var mistypes = isSpellingCorrect(state.getAppContext(), "", languages, control.getText().trim(), false);

      if ((mistypes != null) && (mistypes.length() > 0)) {
        result.addAnnotation(new DefectAnnotation(this, control.getBounds(), mistypes));

        return true;
      }
    }

    if (null != control.getContentDescription() && (null != control.getContentDescription() && control.getContentDescription().trim().length() > 0)) {
      var mistypes = isSpellingCorrect(state.getAppContext(), "", languages, control.getContentDescription().trim(), false);

      if ((mistypes != null) && (mistypes.length() > 0)) {
        result.addAnnotation(new DefectAnnotation(this, control.getBounds(), mistypes));

        return true;
      }
    }

    return false;
  }

  @Override
  public void analyze(@Nonnull AppContext appContext, @Nonnull AppCheckResults results) {
    var messages = appContext.getMessages();

    if (null != messages) {
      var languages = messages.getLanguages().stream().sorted((x, y) -> x.length() - y.length()).toList();

      for (var key : messages.getKeys()) {
        for (var language : languages) {
          var errors = "";
          var message = messages.getMessage(key, language).replace("%s", "");
          var mistypes = isSpellingCorrect(appContext, "", getLanguageByCode(language), message, false);

          errors += " " + mistypes;
          errors = errors.trim();

          if (errors.length() > 0) {
            results.addAnnotation(new AppDefectAnnotation(this, errors));
          }
        }
      }
    }
  }
}
