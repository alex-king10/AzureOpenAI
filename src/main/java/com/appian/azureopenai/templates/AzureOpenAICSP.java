package com.appian.azureopenai.templates;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;


@TemplateId(name="AzureOpenAICSP")
public class AzureOpenAICSP extends SimpleConnectedSystemTemplate {

  public static final String API_KEY = "APIKey";
  public static final String YOUR_RESOURCE_NAME = "YourResourceName";

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
        textProperty(YOUR_RESOURCE_NAME)
            .label("Your Resource Name")
            .instructionText("The name of your Azure OpenAI Resource.")
            .isRequired(true)
            .isImportCustomizable(true)
            .build(),
        textProperty(API_KEY)
            .label("API Key")
            .instructionText("Enter your Azure OpenAI API Key.")
            .masked(true)
            .isRequired(true)
            .isImportCustomizable(true)
            .build()


    );
  }

}
