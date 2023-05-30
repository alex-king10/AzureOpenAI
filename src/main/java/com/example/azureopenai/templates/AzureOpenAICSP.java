package com.example.azureopenai.templates;

//import static std.ConstantKeys.ROOT_URL;

import static com.example.azureopenai.templates.AzureChatCompletionIntegrationTemplate.chatCompletionCall;

import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;


@TemplateId(name="AzureOpenAICSP")
public class AzureOpenAICSP extends SimpleTestableConnectedSystemTemplate {

  public static final String API_KEY = "APIKey";
  public static final String YOUR_RESOURCE_NAME = "YourResourceName";

  public static final String INTEGRATION_CHOICE = "integration_choice";
  public static final String ENDPOINT_INFO = "endpointInfo";

//  what we see and data in those controls
  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    return simpleConfiguration.setProperties(
        textProperty(YOUR_RESOURCE_NAME)
            .label("Your Resource Name")
            .description("The name of your Azure OpenAI Resource.")
            .isRequired(true)
            .isImportCustomizable(true)
            .build(),
        textProperty(API_KEY)
            .label("API Key")
            .description("Enter your Azure OpenAI API Key.")
            .masked(true)
            .isRequired(true)
            .isImportCustomizable(true)
            .build()


    );
  }

  protected TestConnectionResult testConnection(SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {
    String endpoint = "https://openaitestappian2.openai.azure.com/openai/deployments/GPT4_32k/chat/completions?api-version=2023-03-15-preview";
    String apiKey = API_KEY.toString();
    String samplePrompt = "Does Azure OpenAI support customer managed keys?";
    Map<String, Object> inputMap = new HashMap<>();
    inputMap.put("messages", samplePrompt);
    String requestBody = "{\"messages\":[{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},{\"role\": \"user\", \"content\": \"What is azure\"}],\"temperature\": 1.8, \"n\": 2,\n" +
        "\"max_tokens\": 4096, \"presence_penalty\": 1.2, \"frequency_penalty\": 1.4, \"user\": \"me\"}";
    try {
      String response = chatCompletionCall(apiKey, endpoint, inputMap);
    } catch (Exception e) {
//      throw new RuntimeException(e);
      return TestConnectionResult.error(e.getMessage());
    }

    return TestConnectionResult.success();


  }
}
