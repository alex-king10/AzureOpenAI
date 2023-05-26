package com.example.azureopenai.templates;

//import static std.ConstantKeys.ROOT_URL;

import static com.example.azureopenai.templates.AzureChatCompletionIntegrationTemplate.chatCompletionCall;

import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;

import okhttp3.Response;

//import okhttp3.HttpUrl;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;

@TemplateId(name="AzureOpenAICSP")
public class AzureOpenAICSP extends SimpleConnectedSystemTemplate {

  public static final String API_KEY = "APIKey";
  public static final String YOUR_RESOURCE_NAME = "YourResourceName";

  public static final String INTEGRATION_CHOICE = "integration_choice";
  public static final String ENDPOINT_INFO = "endpointInfo";

//  what we see and data in those controls
  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {

    LocalTypeDescriptor endpointInfo = localType(ENDPOINT_INFO).properties(
        textProperty(YOUR_RESOURCE_NAME).label("Resource Name").isRequired(true).build()
//        textProperty(DEPLOYMENT_ID).label("Deployment Id").isRequired(true).build(),
//        textProperty(API_VERSION).label("API Version").isRequired(true).build(),
//        textProperty(INTEGRATION_CHOICE).label("Integration").choices(
//            Choice.builder().name("Completion").value("completions").build(),
//            Choice.builder().name("Embedding").value("embeddings").build(),
//            Choice.builder().name("Chat Completion").value("chat/completions").build()
//        ).build()
    ).build();

    return simpleConfiguration.setProperties(
        // Make sure you make public constants for all keys so that associated
        // integrations can easily access this field
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

//  @Override
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
