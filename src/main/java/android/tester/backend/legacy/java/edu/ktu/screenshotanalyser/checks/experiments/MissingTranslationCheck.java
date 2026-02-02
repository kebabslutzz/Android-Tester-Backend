package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.experiments;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.*;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.AppContext;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import org.languagetool.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MissingTranslationCheck extends BaseTextRuleCheck implements IAppRuleChecker, IStateRuleChecker {
  public MissingTranslationCheck() {
    super(14, "SL1");
  }

  @Override
  public void analyze(AppContext appContext, AppCheckResults results) {
    var messages = appContext.getMessages();
    var languages = messages.getLanguages().stream().sorted((x, y) -> x.length() - y.length()).toArray();

    for (var key : messages.getKeys()) {
      var translations = messages.getTranslations(key);
      var missingLanguages = new StringBuilder();
      var sameTranslations = new HashMap<String, HashSet<String>>();

      for (int i = 0; i < languages.length; i++) {
        var sourceLanguage = (String) languages[i];
        var sourceMessage = translations.get(sourceLanguage);

        if (null == sourceMessage) {
          missingLanguages.append(" " + sourceLanguage);

          continue;
        }

        if (!isTranslateable(sourceMessage, appContext)) {
          continue;
        }

        for (int j = i + 1; j < languages.length; j++) {
          var targetLanguage = (String) languages[j];

          if (targetLanguage.startsWith(sourceLanguage.substring(0, 2) + "-")) {
            break;
          }

          var targetMessage = translations.get(targetLanguage);

          if (null == sourceMessage) {
            missingLanguages.append(" " + targetLanguage);

            continue;
          }

          if (sourceMessage.equals(targetMessage)) {
            if ((!isSpellingCorrect(sourceLanguage, sourceMessage.toLowerCase(), appContext)) || (!isSpellingCorrect(targetLanguage, targetMessage.toLowerCase(), appContext))) {
              var sameLanguages = sameTranslations.get(sourceMessage);

              if (null == sameLanguages) {
                sameLanguages = new HashSet<>();

                sameTranslations.put(sourceMessage, sameLanguages);
              }

              sameLanguages.add(sourceLanguage);
              sameLanguages.add(targetLanguage);
            }
          }
        }
      }

      //missingLanguages = missingLanguages.trim();

      for (var sameTranslation : sameTranslations.keySet()) {
        var sameLanguages = sameTranslations.get(sameTranslation).stream().collect(Collectors.joining(","));

        results.addAnnotation(new AppDefectAnnotation(this, "Same for [" + key + "]: [" + sameTranslation + "]: " + sameLanguages));
      }
    }
  }

  @Override
  public void analyze(State state, StateCheckResults results) {
    var placeholders = new ArrayList<String>();
    var allTexts = state.getActualControls().stream().map(this::getText).filter(x -> x != null && x.length() > 0).collect(Collectors.joining(". "));
    var languages = getLanguage(allTexts);

    for (var control : state.getActualControls()) {
      if ((isTranslateable(control.getText(), state.getAppContext())) && (isPlaceholder(control.getText(), state, languages))) {
        placeholders.add(control.getText());

        results.addAnnotation(new DefectAnnotation(this, control.getBounds(), "unstranslated: " + control.getText()));
      }
    }
  }

  private boolean isPlaceholder(String message, State state, List<Language> stateLanguages) {
    if ((null == message) || (message.length() == 0)) {
      return false;
    }

    if (isUpperCase(message)) {
      return false;
    }

    var words = message.split(" ");

    if (words.length > 1) {
      return false;
    }

    var messages = state.getAppContext().getMessages();

    if (null != messages) {
      var keys = messages.getKeys();

      if (keys.contains(message)) {
        return !isSpellingCorrect(message, stateLanguages);
				/*

				var aa = messages.getTranslations(message);

				for (var key : aa.keySet())
				{
					var translation = aa.get(key);

					var languages = getLanguageByCode(key);

					for (var language : languages)
					{

					JLanguageTool langTool = new JLanguageTool(language);

						List<RuleMatch> matches = new ArrayList<>();
						try
						{
							matches = langTool.check(translation);
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (RuleMatch match : matches) {

							System.out.println("Grammar check failed: " + match.getMessage()

							//+". Possible fixes: "+match.getSuggestedReplacements()

							+". Text was: "+translation);


					//		results.add(CheckResult.Nok(type, "Grammar check failed: " + match.getMessage()+". Possible fixes: "+match.getSuggestedReplacements()+". Text was: "+resourceText.getValue(),
			//					resourceText.getFile() + "@" + resourceText.getKey(), langKey));

							return true;
						}



					}


				}
				*/


        //	return aa != null;  //true;
      }
    }

    return false;
  }
}
