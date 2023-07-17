package com.appian.azureopenai.templates;

import static std.ConstantKeys.API_VERSION;
import static std.ConstantKeys.DEPLOYMENT_ID;
import static std.ConstantKeys.FREQUENCY_PENALTY;
import static std.ConstantKeys.LOGIT_BIAS;
import static std.ConstantKeys.MAX_TOKENS;
import static std.ConstantKeys.N;
import static std.ConstantKeys.PRESENCE_PENALTY;
import static std.ConstantKeys.TEMPERATURE;
import static std.ConstantKeys.USER;
import static std.SharedMethods.getErrorDetails;

//import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.BooleanDisplayMode;
import com.appian.connectedsystems.templateframework.sdk.configuration.LocalTypeDescriptor;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.TypeReference;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@TemplateId(name="AzureChatCompletionIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureChatCompletionIntegrationTemplate extends SimpleIntegrationTemplate {
  static Map<String,Object> requestDiagnostic = new HashMap<>();
  static Gson gson = new Gson();

  private static Map<String, Object> getFullResponseObject(String responseBody) {
    Type type = new TypeToken<Map<String, Object>>(){}.getType();

    Map<String, Object> myMap = gson.fromJson(responseBody, type);
    myMap.put("Chat Completions", getResponseContent(responseBody));
    return myMap;

  }

  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/chat/completions?api-version=%s", resourceName, deploymentID, APIVersion);
  }
  //      retrieve content from reply
  private static ArrayList<String> getResponseContent(String responseBody) {
//    handle JSON error here
    JSONObject jsonResponse = new JSONObject(responseBody);
    JSONArray choices = jsonResponse.getJSONArray("choices");
    ArrayList<String> contentArr = new ArrayList<>();

    if (choices.length() > 0) {
      for (int i = 0; i < choices.length(); i++) {
        JSONObject choice = choices.getJSONObject(i);
        JSONObject message = choice.getJSONObject("message");
        contentArr.add(message.getString("content"));
      }
    }
    return contentArr;
  }

  public static String chatCompletionCall(String apiKey, String endpoint, Map<String, Object> inputMap)
      throws Exception {
    OkHttpClient client = new OkHttpClient();
    Map<String, Object> requestMap = new HashMap<>();

    for (String config: inputMap.keySet()) {
      Object value = inputMap.get(config);
      if (value != null) {
        requestMap.put(config, value);
        requestDiagnostic.put(config, value);
      }
    }

    String requestBody = gson.toJson(requestMap);

    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestBody);
    String responseBody;

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();
//    Response response = null;

//    try {
      try (Response response = client.newCall(request).execute() ) {
//      response = client.newCall(request).execute();
      responseBody = response.body().string();
//      } catch (IOException e) {
//        testing catching and throwing bad input errors
      } catch (Exception e) {
//      response.close();
//      return e.toString();
        throw new Exception(e);
    }
//      finally {
//      if (response != null)
//        response.close();
//    }
    return responseBody;

  }


  public static final String MESSAGE = "messages";

  public static final String DEV_SETTINGS = "devSettings";




  @Override
  protected SimpleConfiguration getConfiguration(
    SimpleConfiguration integrationConfiguration,
    SimpleConfiguration connectedSystemConfiguration,
    PropertyPath propertyPath,
    ExecutionContext executionContext) {

//    create custom type for prompt
    LocalTypeDescriptor messageInputType = localType("messageInput").properties(
        textProperty("role").label("Role").build(),
        textProperty("content").label("Content").build()
    ).build();
    localTypeProperty(messageInputType, "neededToChat");

//    create dynamic fields
    Boolean devSettingsState = integrationConfiguration.getValue(DEV_SETTINGS);
    if(devSettingsState == null || !devSettingsState) {
      return integrationConfiguration.setProperties(
        textProperty(DEPLOYMENT_ID).label("Deployment ID")
            .description("The deployment name you chose when you deployed the model. This will be specific to different models deployed in your account.")
            .placeholder("ex: GPT4_32K")
            .instructionText("Name of your deployment ID for this integration's appropriate model.")
            .isRequired(true)
            .isExpressionable(true)
            .build(),
          textProperty(API_VERSION).label("API Version")
              .description("The API version to use for this operation. This follows the YYYY-MM-DD format.")
              .instructionText("API version to use for this operation.")
              .isRequired(true)
              .isExpressionable(true)
              .placeholder("ex: 2023-05-15")
              .build(),
          listTypeProperty(MESSAGE).label("Message Input")
              .itemType(TypeReference.from(messageInputType))
              .isExpressionable(true)
              .isRequired(true)
              .description("The messages to generate chat completions for, in the chat format.")
              .instructionText("The messages to generate chat completions for, in the chat format: " +
                  "{{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                  "    {\"role\": \"user\", \"content\": \"What does Appian's platform do?\"}}")
              .build(),
          booleanProperty(DEV_SETTINGS).label("Developer Settings")
              .displayMode(BooleanDisplayMode.CHECKBOX)
              .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
              .refresh(RefreshPolicy.ALWAYS)
              .build()
      );
    }


    return integrationConfiguration.setProperties(
        textProperty(DEPLOYMENT_ID).label("Deployment ID")
            .description("The deployment name you chose when you deployed the model. This will be specific to different models deployed in your account.")
            .instructionText("Name of your deployment ID for this integration's appropriate model.")
            .placeholder("ex: GPT4_32K")
            .isRequired(true)
            .isExpressionable(true)
            .build(),
        textProperty(API_VERSION).label("API Version")
            .description("The API version to use for this operation. This follows the YYYY-MM-DD format.")
            .instructionText("API version to use for this operation.")
            .isRequired(true)
            .isExpressionable(true)
            .placeholder("ex: 2023-05-15")
            .build(),
        listTypeProperty(MESSAGE).label("Message Input")
            .itemType(TypeReference.from(messageInputType))
            .isExpressionable(true)
            .isRequired(true)
            .description("The messages to generate chat completions for, in the chat format.")
            .instructionText("The messages to generate chat completions for, in the chat format: " +
                "{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                "    {\"role\": \"user\", \"content\": \"What does Appian's platform do?\"}")
            .build(),

        booleanProperty(DEV_SETTINGS).label("Developer Settings")
            .displayMode(BooleanDisplayMode.CHECKBOX)
            .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
            .refresh(RefreshPolicy.ALWAYS)
            .build(),
        doubleProperty(TEMPERATURE).label("Temperature")
            .isRequired(false)
            .instructionText("Sampling temperature to use, between 0 and 2")
            .isExpressionable(true)
            .description("What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.\\nWe generally recommend altering this or top_p but not both. Default of 1.")
            .placeholder("1.0")
            .build(),
        integerProperty(N).label("n")
            .isRequired(false)
            .instructionText("Number of chat completion choices to generate for each input message.")
            .isExpressionable(true)
            .description("Default of 1.")
            .placeholder("1")
            .build(),
        integerProperty(MAX_TOKENS).label("Max Tokens")
            .isRequired(false)
            .isExpressionable(true)
            .instructionText("Maximum number of tokens to generate in the completion.")
            .placeholder("4096")
            .description("Token count of your prompt plus max_tokens can't exceed the model's context length. Default of 4096.")
            .build(),
        doubleProperty(PRESENCE_PENALTY).label("Presence Penalty")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("0.0")
            .description("Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.")
            .instructionText("Number between -2.0 and 2.0.")
            .build(),
        doubleProperty(FREQUENCY_PENALTY).label("Frequency Penalty")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("0.0")
            .instructionText("Number between -2.0 and 2.0.")
            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
            .build(),
        textProperty(LOGIT_BIAS).label("Logit Bias")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("{}")
            .description("Modify the likelihood of specified tokens appearing in the completion. Accepts a json object that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.")
            .instructionText("Enter Logit Bias as JSON of token to bias value from -100 to 100.")
            .build(),
        textProperty(USER).label("User")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("firstName.lastName")
            .description("A unique identifier representing your end-user, which can help Azure OpenAI to monitor and detect abuse.")
            .instructionText("Enter a username as a unique identifier")
            .build()
    );
  }


  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
//    1. set up step
//      retrieve data from CSP
    String apiKey = connectedSystemConfiguration.getValue(AzureOpenAICSP.API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(AzureOpenAICSP.YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);


    Map<String, Object> inputMap = new HashMap<>();

    requestDiagnostic.put("Endpoint", endpoint);
//    retrieve data from integration

//      messages
    String role;
    String content;

    List<Object> messages = integrationConfiguration.getValue(MESSAGE);
    ArrayList<Map<String, String>> messageList = new ArrayList<>();
    HashMap<String, String> messageMap;
    for (int i = 0; i < messages.size(); i++) {
      messageMap = new HashMap<>();
      role = (String)((PropertyState)((Map<String, Object>)((PropertyState)messages.get(i)).getValue()).get("role")).getValue();
      content = (String)((PropertyState)((Map<String, Object>)((PropertyState)messages.get(i)).getValue()).get("content")).getValue();
      messageMap.put("role", role);
      messageMap.put("content", content);
      messageList.add(messageMap);
    }


    inputMap.put("messages", messageList);

//      temperature
    Double temperature =  integrationConfiguration.getValue(TEMPERATURE);
    inputMap.put("temperature", temperature);

//      n
    Integer nInt = integrationConfiguration.getValue(N);
    inputMap.put("n", nInt);

//    max_tokens
    Integer max_tokens = integrationConfiguration.getValue(MAX_TOKENS);
    inputMap.put("max_tokens", max_tokens);

//    presence_penalty
    Double presencePenNum = integrationConfiguration.getValue(PRESENCE_PENALTY);
    inputMap.put("presence_penalty", presencePenNum);

//    frequency_penalty
    Double freqPenNum= integrationConfiguration.getValue(FREQUENCY_PENALTY);
    inputMap.put("frequency_penalty", freqPenNum);

//    logit_bias
    String logitBias = integrationConfiguration.getValue(LOGIT_BIAS);
    inputMap.put("logit_bias", logitBias);

//    user
    String user = integrationConfiguration.getValue(USER);
    inputMap.put("user", user);


    Map<String,Object> resultMap = new HashMap<>();
    Map<String, Object> diagnosticResponse = new HashMap<>();
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();

//    2. make the remote request
    String response ="";
    final long start = System.currentTimeMillis();
    IntegrationError error = null;
    final IntegrationDesignerDiagnostic diagnostic;

    try {
//      type String
      response = chatCompletionCall(apiKey, endpoint, inputMap);
      resultMap.put("Response", getFullResponseObject(response));

    } catch (Exception e) {
//      e.cause.detailMessage: "timeout"
//      e.cause.cause: "java.net. ..."
      if (response.equals("")) {
        response = String.valueOf(e.getCause());
        error = IntegrationError.builder()
            .title("Error Title: " + response)
            .message("While calling the Chat Completion endpoint, an error occurred. Please check your inputs and ensure your number of tokens has not exceeded the limit set by Azure OpenAI.\n")
            .detail(response)
            .build();
      } else {
        String[] errorDetails = getErrorDetails(response);
        error = IntegrationError.builder()
            .title("Error Title: " + errorDetails[0])
            .message("While calling the Chat Completion endpoint, an error occurred. Please check your Deployment ID and API Version. The following message was returned from the API request:\n")
            .detail(errorDetails[1])
            .build();
      }



    } finally {

      //    3. translate resultMap from integration into appian values
      final long end = System.currentTimeMillis();
      final long executionTime = end - start;

       diagnostic = diagnosticBuilder
          .addExecutionTimeDiagnostic(executionTime)
          .addRequestDiagnostic(requestDiagnostic)
          .addResponseDiagnostic(diagnosticResponse)
          .addResponseDiagnostic(diagnosticResponse)
          .build();

    }
    if (error != null) {
      return IntegrationResponse
          .forError(error)
          .withDiagnostic(diagnostic)
          .build();
    }
    

    resultMap.put("Successful Response Code", 200);
    
    return IntegrationResponse
        .forSuccess(resultMap)
        .withDiagnostic(diagnostic)
        .build();

  }
}
