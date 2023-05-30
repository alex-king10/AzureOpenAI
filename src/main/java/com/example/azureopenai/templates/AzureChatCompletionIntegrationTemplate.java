package com.example.azureopenai.templates;

import static com.example.azureopenai.templates.AzureOpenAICSP.API_KEY;
import static com.example.azureopenai.templates.AzureOpenAICSP.YOUR_RESOURCE_NAME;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Must provide an integration id. This value need only be unique for this connected system
@TemplateId(name="AzureChatCompletionIntegrationTemplate")
// Set template type to READ since this integration does not have side effects
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureChatCompletionIntegrationTemplate extends SimpleIntegrationTemplate {

//  private static String[] getErrorDetails(String responseBdoy) {
//    JSONObject responseJSON = new JSONObject(responseBdoy);
//    String errorTitle = (String)((JSONObject)responseJSON.get("error")).get("code");
//    String message = (String)((JSONObject)responseJSON.get("error")).get("message");
//    return new String[] {errorTitle, message};
//  }
  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/chat/completions?api-version=%s", resourceName, deploymentID, APIVersion);
  }
  //      retrieve content from reply
  private static ArrayList<String> getResponseContent(String responseBody) {
    String responseStr = responseBody;
    JSONObject jsonResponse = new JSONObject(responseStr);
    JSONArray choices = jsonResponse.getJSONArray("choices");
    ArrayList<String> contentArr = new ArrayList<>();
    String content = "";

    if (choices.length() > 0) {
      for (int i = 0; i < choices.length(); i++) {
        JSONObject choice = choices.getJSONObject(i);
        JSONObject message = choice.getJSONObject("message");
        contentArr.add(message.getString("content"));
//        content = message.getString("content");
      }
    }
    if (choices.length() > 0) {
      JSONObject choice = choices.getJSONObject(0);
      JSONObject message = choice.getJSONObject("message");
      content = message.getString("content");
    }
    return contentArr;
  }

  public static String chatCompletionCall(String apiKey, String endpoint, Map<String, Object> inputMap) throws Exception {
    OkHttpClient client = new OkHttpClient();
    String prompt = (String)inputMap.get("messages");
    Double temperature = (Double)inputMap.get("temperature");
    Integer n = (Integer)inputMap.get("n");
    Integer max_tokens = (Integer)inputMap.get("max_tokens");
    Double presence_pen = (Double)inputMap.get("presence_penalty");
    Double frequency_pen = (Double)inputMap.get("frequency_penalty");
    String logit_bias = (String)inputMap.get("logit_bias");
    String user = (String)inputMap.get("user");

    String requestBody = String.format(
        "{\"messages\": %s, \"temperature\": %f, \"n\": %d," +
          "\"max_tokens\": %d, \"presence_penalty\": %f, \"frequency_penalty\": %f," +
          "\"logit_bias\": %s, \"user\": \"%s\"}",
        prompt, temperature, n, max_tokens, presence_pen, frequency_pen,
        logit_bias, user);


    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestBody);
    String responseBody;

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();

    Response response = null;
//    try {
    response = client.newCall(request).execute();
    responseBody = response.body().string();
//    } catch (Exception e) {
//      return response.body().string();
//    }

    
//tested here for error details - call from here?
    response.close();

    return responseBody;

  }


  public static final String MESSAGE = "messages";
//  public static final String TEMPERATURE = "temperature";
//  public static final String N = "n";
//  public static final String MAX_TOKENS = "maxTokens";
//  public static final String PRESENCE_PENALTY = "presencePenalty";
//  public static final String FREQ_PENALTY = "freqPenalty";
//  public static final String LOGIT_BIAS = "logitBias";
//  public static final String USER = "user";
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
      textProperty(DEPLOYMENT_ID)
          .description("The deployment name you chose when you deployed the model. This will be specific to different models deployed in your account.")
          .placeholder("ex: GPT4_32K")
          .isRequired(true)
          .isExpressionable(true)
          .build(),
          textProperty(API_VERSION)
              .description("The API version to use for this operation. This follows the YYYY-MM-DD format.")
              .isRequired(true)
              .isExpressionable(true)
              .placeholder("ex: 2023-05-15")
              .build(),
          listTypeProperty(MESSAGE).label("Message Input")
              .itemType(TypeReference.from(messageInputType))
              .isExpressionable(true)
              .isRequired(true)
              .description("This will be your initial message to Azure's OpenAI Chat Bot")
              .build(),
          booleanProperty(DEV_SETTINGS).label("Developer Settings")
              .displayMode(BooleanDisplayMode.CHECKBOX)
              .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
              .refresh(RefreshPolicy.ALWAYS)
              .build()
      );
    }

//    LocalTypeDescriptor chatCompConfig = localType("chatCompConfig").properties(
//        listTypeProperty(MESSAGE).label("Message Input")
//            .itemType(TypeReference.from(messageInputType))
//            .isExpressionable(true)
//            .isRequired(true)
//            .description("This will be your initial message to Azure's OpenAI Chat Bot")
//            .build(),
//        textProperty(TEMPERATURE).label("Temperature")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic. Default of 1.")
//            .build(),
//        integerProperty(N).label("n")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("How many chat completion choices to generate for each input message. Default of 1.")
//            .build(),
//        integerProperty(MAX_TOKENS).label("Max Tokens")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("The maximum number of tokens allowed for the generated answer. By default, the number of tokens the model can return will be 4096 - prompt tokens.")
//            .build(),
//        textProperty(PRESENCE_PENALTY).label("Presence Penalty")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.")
//            .build(),
//        textProperty(FREQ_PENALTY).label("Frequency Penalty")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
//            .build(),
//        textProperty(LOGIT_BIAS).label("Logit Bias")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("Modify the likelihood of specified tokens appearing in the completion. Accepts a json object that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.")
//            .build(),
//        textProperty(USER).label("User")
//            .isRequired(false)
//            .isExpressionable(true)
//            .description("A unique identifier representing your end-user, which can help Azure OpenAI to monitor and detect abuse.")
//            .build()
//        ).build();
//
//    return integrationConfiguration.setProperties(
//        localTypeProperty(chatCompConfig).label("Chat Completion Choices").isExpressionable(true).build()
//    );

    return integrationConfiguration.setProperties(
        // Make sure you make constants for all keys so that you can easily
        // access the values during execution
        textProperty(DEPLOYMENT_ID).label("Deployment ID")
            .description("The deployment name you chose when you deployed the model. This will be specific to different models deployed in your account.")
            .placeholder("ex: GPT4_32K")
            .isRequired(true)
            .isExpressionable(true)
            .build(),
        textProperty(API_VERSION).label("API Version")
            .description("The API version to use for this operation. This follows the YYYY-MM-DD format.")
            .isRequired(true)
            .isExpressionable(true)
            .placeholder("ex: 2023-05-15")
            .build(),
        listTypeProperty(MESSAGE).label("Message Input")
            .itemType(TypeReference.from(messageInputType))
            .isExpressionable(true)
            .isRequired(true)
            .description("This will be your initial message to Azure's OpenAI Chat Bot")
            .build(),
        booleanProperty(DEV_SETTINGS).label("Developer Settings")
            .displayMode(BooleanDisplayMode.CHECKBOX)
            .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
            .refresh(RefreshPolicy.ALWAYS)
            .build(),
        textProperty(TEMPERATURE).label("Temperature")
            .isRequired(false)
            .isExpressionable(true)
            .description("What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic. Default of 1.")
            .placeholder("1.0")
            .build(),
        integerProperty(N).label("n")
            .isRequired(false)
            .isExpressionable(true)
            .description("How many chat completion choices to generate for each input message. Default of 1.")
            .placeholder("1")
            .build(),
        integerProperty(MAX_TOKENS).label("Max Tokens")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("4096")
            .description("The maximum number of tokens allowed for the generated answer. By default, the number of tokens the model can return will be 4096 - prompt tokens.")
            .build(),
        textProperty(PRESENCE_PENALTY).label("Presence Penalty")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("0.0")
            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.")
            .build(),
        textProperty(FREQUENCY_PENALTY).label("Frequency Penalty")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("0.0")
            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
            .build(),
        textProperty(LOGIT_BIAS).label("Logit Bias")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("{}")
            .description("Modify the likelihood of specified tokens appearing in the completion. Accepts a json object that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.")
            .build(),
        textProperty(USER).label("User")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("firstName.lastName")
            .description("A unique identifier representing your end-user, which can help Azure OpenAI to monitor and detect abuse.")
            .build()
    );
  }

//  called when designer clicks on test button
//  returning an integration response
  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
//    1. set up step
//      retrieve data from CSP
    Map<String,Object> requestDiagnostic = new HashMap<>();
    String apiKey = connectedSystemConfiguration.getValue(API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);


    Map<String, Object> inputMap = new HashMap<>();

    requestDiagnostic.put("Endpoint", endpoint);
//    retrieve data from integration
//      messages

    List<Object> messages;
    String role;
    String content;
    String messageString = "[";
    String itemString;

    try {
//      retrieve list property object
      messages = integrationConfiguration.getValue(MESSAGE);
//      retrieve list item data (local type) --> Map<String, Object>
      for (int i = 0; i < messages.size(); i++) {
        role = (String)((PropertyState)((Map<String, Object>)((PropertyState)messages.get(i)).getValue()).get("role")).getValue();
        content = (String)((PropertyState)((Map<String, Object>)((PropertyState)messages.get(i)).getValue()).get("content")).getValue();

        itemString = String.format("{\"role\": \"%s\", \"content\": \"%s\"}", role, content);
        messageString += itemString;

        if (i < messages.size() - 1) { messageString += ","; }
        else { messageString += "]"; }

      }

      System.out.println(messageString);

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    requestDiagnostic.put("API Key", apiKey == null ? null: "********");
    requestDiagnostic.put("Message Input", messageString);
    inputMap.put("messages", messageString);

//      temperature
    Double temperature = 1.0;
    String temperatureStr = integrationConfiguration.getValue(TEMPERATURE);
    if (temperatureStr!=null) { temperature = Double.valueOf(temperatureStr);}
    if (temperature < 0.0 || temperature > 2.0) { temperature = 1.0; }
    //    else { alert("Temperature must be between 0 and 2."); }
    inputMap.put("temperature", temperature);
    requestDiagnostic.put("Temperature", temperature);


//      n
    Integer nInt = integrationConfiguration.getValue(N);
    if(nInt == null) nInt = 1;
    inputMap.put("n", nInt);
    requestDiagnostic.put("N", nInt);

//    max_tokens
    Integer max_tokens = integrationConfiguration.getValue(MAX_TOKENS);
    if(max_tokens == null) max_tokens = 4096;
    inputMap.put("max_tokens", max_tokens);
    requestDiagnostic.put("Maxiumum Number of Tokens", max_tokens);

//    presence_penalty
    String presencePen = integrationConfiguration.getValue(PRESENCE_PENALTY);
    double presencePenNum = 0.0;
    //    if value was given convert to double
    if(presencePen != null) presencePenNum = Double.valueOf(presencePen);
    //    if value not between -2 and 2, set to default
    if (presencePenNum > 2.0 || presencePenNum < -2.0) {
      presencePenNum = 0.0;
    }
    inputMap.put("presence_penalty", presencePenNum);
    requestDiagnostic.put("Presence Penalty", presencePenNum);

//    frequency_penalty
    String frequencyPen = integrationConfiguration.getValue(FREQUENCY_PENALTY);
    double freqPenNum= 0.0;
    //    if value was given convert to double
    if(frequencyPen != null) freqPenNum = Double.valueOf(frequencyPen);
    //    if value not between -2 and 2, set to default
    if (freqPenNum > 2.0 || freqPenNum < -2.0) {
      freqPenNum = 0.0;
    }
    inputMap.put("frequency_penalty", freqPenNum);
    requestDiagnostic.put("Frequency Penalty", freqPenNum);

//    logit_bias
    String logitBias = integrationConfiguration.getValue(LOGIT_BIAS);
    if (logitBias == null) logitBias = new JSONObject().toString();
    inputMap.put("logit_bias", logitBias);
    requestDiagnostic.put("Logit Bias", logitBias);

//    user
    String user = integrationConfiguration.getValue(USER);
    if (user == null) user = "";
    inputMap.put("user", user);
    requestDiagnostic.put("user", user);


    Map<String,Object> resultMap = new HashMap<>();
    Map<String, Object> diagnosticResponse = new HashMap<>();
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();

//    2. make the remote request
    String response ="";
    ArrayList<String> result = new ArrayList<>();
    final long start = System.currentTimeMillis();
    IntegrationError error = null;
    final IntegrationDesignerDiagnostic diagnostic;

    try {
//      type Response
      response = chatCompletionCall(apiKey, endpoint, inputMap);
      resultMap.put("Chat Completion", getResponseContent(response));

    } catch (Exception e) {

      String[] errorDetails = getErrorDetails(response);
      error = IntegrationError.builder()
              .title("Error Title: " + errorDetails[0])
              .message("While calling the Chat Completion endpoint, an error occurred. Please check your Deployment ID and API Version.\n")
              .detail(errorDetails[1])
              .build();

    } finally {

      diagnosticResponse.put("Full Response", response);

      //    3. translate resultMap from integration into appian values

      final long end = System.currentTimeMillis();
      final long executionTime = end - start;

       diagnostic = diagnosticBuilder
          .addExecutionTimeDiagnostic(executionTime)
          .addRequestDiagnostic(requestDiagnostic)
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
