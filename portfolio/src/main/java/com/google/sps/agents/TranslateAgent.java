package com.google.sps.agents;
 
// Imports the Google Cloud client library
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.translate.*;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.data.Output;
import com.google.sps.agents.Agent;
import com.google.sps.utils.AgentUtils;
import java.io.IOException;
import java.util.Map;
 
/**
 * Translate Agent
 */
public class TranslateAgent implements Agent {
    private final String intentName;
  	private String text;
    private String languageTo;
    private String languageFrom;
    private String languageToCode;
    private String languageFromCode;
 
    public TranslateAgent(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
        System.out.println(parameters);
        text = parameters.get("text").getStringValue();
        languageTo = parameters.get("lang-to").getStringValue();
        languageFrom = parameters.get("lang-from").getStringValue();
 
        if (languageFrom == "") {
            languageFrom = "English";
        }

        languageToCode = AgentUtils.getLanguageCode(languageTo);
        languageFromCode = AgentUtils.getLanguageCode(languageFrom);
	}
	
	@Override
	public String getOutput() {
        Translation translation = translate(text, languageFromCode, languageToCode);
        String translatedString = translation.getTranslatedText();
	    return text + " in " + languageTo + " is: " + translatedString;
	}

	@Override
	public String getDisplay() {
		return null;
	}

	@Override
	public String getRedirect() {
		return null;
    }

    public static Translation translate(String text, String languageFromCode, String languageToCode) {
        Translate translate = TranslateOptions.getDefaultInstance().getService();

        Translation translation =
        translate.translate(
            text,
            Translate.TranslateOption.sourceLanguage(languageFromCode),
            Translate.TranslateOption.targetLanguage(languageToCode),
            // Use "base" for standard edition, "nmt" for the premium model.
            Translate.TranslateOption.model("nmt"));

        System.out.printf("TranslatedText:\nText: %s\n", translation.getTranslatedText());
        return translation;
    }



}