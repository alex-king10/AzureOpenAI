package com.appian.azureopenai.templates;

import static std.ConstantKeys.API_VERSION;
import static std.ConstantKeys.DEPLOYMENT_ID;
import static std.ConstantKeys.FREQUENCY_PENALTY;
import static std.ConstantKeys.LOGIT_BIAS;
import static std.ConstantKeys.MAX_TOKENS;
import static std.ConstantKeys.N;
import static std.ConstantKeys.PRESENCE_PENALTY;
import static std.ConstantKeys.STOP;
import static std.ConstantKeys.TEMPERATURE;
import static std.ConstantKeys.USER;
import static std.SharedMethods.getErrorDetails;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyState;
import com.appian.connectedsystems.templateframework.sdk.configuration.RefreshPolicy;
import com.appian.connectedsystems.templateframework.sdk.configuration.SystemType;
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

@TemplateId(name="AzureCompletionIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureCompletionIntegrationTemplate extends SimpleIntegrationTemplate {


  public static final String PROMPT = "prompt";
  public static final String DEV_SETTINGS = "devSettings";
  public static final String TOP_P = "top_p";
  public static final String LOGPROBS = "logprobs";
  public static final String SUFFIX = "suffix";
  public static final String ECHO = "echo";
  public static final String BEST_OF = "best_of";
  static Map<String,Object> requestDiagnostic = new HashMap<>();
  static Gson gson = new Gson();



  private static String completionAPICall(String apiKey, String endpoint, HashMap<String, Object> inputMap) {

    Map<String, Object> requestMap = new HashMap<>();

    for (String config: inputMap.keySet()) {
      Object value = inputMap.get(config);
      if (value != null) {
        requestMap.put(config, value);
        requestDiagnostic.put(config, value);
      }
    }

    String requestString = gson.toJson(requestMap);



    OkHttpClient client = new OkHttpClient();
    String requestBody;

//    TODO try converting with the JSON thing

    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestString);

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();

    Response response;
    String responseBody;
    try {
      response = client.newCall(request).execute();
      responseBody = response.body().string();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    response.close();

    return responseBody;
  }


  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/completions?api-version=%s", resourceName, deploymentID, APIVersion);
  }

  private static Map<String, Object> getFullResponseObject(String responseBody) {
    Type type = new TypeToken<Map<String, Object>>(){}.getType();

    Map<String, Object> myMap = gson.fromJson(responseBody, type);
    myMap.put("Completions", getResponseContent(responseBody));
    return myMap;

  }

  private static ArrayList<String> getResponseContent(String responseBody) {
    JSONObject jsonResponse = new JSONObject(responseBody);
    JSONArray choices = jsonResponse.getJSONArray("choices");
    ArrayList<String> contentArr = new ArrayList<>();

    if (choices.length() > 0) {
      for (int i = 0; i < choices.length(); i++) {
        JSONObject choice = choices.getJSONObject(i);
        contentArr.add(choice.getString("text"));

      }
    }

    return contentArr;
  }

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {

    Boolean devSettingState = integrationConfiguration.getValue(DEV_SETTINGS);
    System.out.println(devSettingState);

    if (devSettingState == null || !devSettingState) {
      return integrationConfiguration.setProperties(
          textProperty(DEPLOYMENT_ID).label("Deployment ID")
              .description("The deployment name you chose when you deployed the model. This will be specific to different models deployed in your account.")
              .placeholder("ex: GPT35Turbo_DeploymentID")
              .isRequired(true)
              .isExpressionable(true)
              .instructionText("Name of your deployment ID for this integration's appropriate model.")
              .build(),
          textProperty(API_VERSION).label("API Version")
              .description("This follows the YYYY-MM-DD format.")
              .instructionText("API version to use for this operation.")
              .isRequired(true)
              .isExpressionable(true)
              .placeholder("ex: 2023-05-15")
              .build(),
          listTypeProperty(PROMPT).label("Prompt")
              .itemType(SystemType.STRING)
              .isRequired(false)
              .isExpressionable(true)
              .description("Default is the beginning of a new document.")
              .instructionText("Prompt(s) to generate completions for, encoded as a list of strings. In the following format: \n" +
                  "{\n\t\"Once upon a time...\",\n\t\"In a land far away\"\n }")
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
            .placeholder("ex: GPT35Turbo_DeploymentID")
            .isRequired(true)
            .isExpressionable(true)
            .instructionText("Name of your deployment ID for this integration's appropriate model.")
            .build(),
        textProperty(API_VERSION).label("API Version")
            .description("This follows the YYYY-MM-DD format.")
            .instructionText("API version to use for this operation.")
            .isRequired(true)
            .isExpressionable(true)
            .placeholder("ex: 2023-05-15")
            .build(),
        listTypeProperty(PROMPT).label("Prompt")
            .itemType(SystemType.STRING)
            .isRequired(false)
            .isExpressionable(true)
            .description("Default is the beginning of a new document.")
            .instructionText("Prompt(s) to generate completions for, encoded as a list of strings. In the following format: \n" +
                "{\n\t\"Once upon a time...\",\n\t\"In a land far away\"\n }")
            .build(),
        booleanProperty(DEV_SETTINGS).label("Developer Settings")
            .displayMode(BooleanDisplayMode.CHECKBOX)
            .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
            .refresh(RefreshPolicy.ALWAYS)
            .build(),
        integerProperty(MAX_TOKENS).label("Max Tokens")
            .isRequired(false)
            .isExpressionable(true)
            .instructionText("Maximum number of tokens to generate in the completion.")
            .placeholder("16")
            .description("The token count of your prompt plus max_tokens can't exceed the model's context length. Default of 16.")
            .build(),
        doubleProperty(TEMPERATURE).label("Temperature")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("1.0")
            .instructionText("Sampling temperature to use, between 0 and 2")
            .description("Higher values means the model will take more risks. Default of 1.")
            .build(),
        doubleProperty(TOP_P).label("Top P")
            .isRequired(false)
            .isExpressionable(true)
            .description("An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. We generally recommend altering this or temperature but not both.")
            .placeholder("1.0")
            .instructionText("Top p value between 0 and 1, nucleus sampling")
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
            .build(),
        integerProperty(N).label("n")
            .isRequired(false)
            .isExpressionable(true)
            .description("Default of 1.")
            .instructionText("Number of chat completion choices to generate for each input message.")
            .placeholder("1")
            .build(),
        integerProperty(LOGPROBS).label("Log Probs")
            .isRequired(false)
            .isExpressionable(true)
            .description("Include the log probabilities on the logprobs most likely tokens, as well the chosen tokens. For example, if logprobs is 10, the API will return a list of the 10 most likely tokens. the API will always return the logprob of the sampled token, so there may be up to logprobs+1 elements in the response. This parameter cannot be used with gpt-35-turbo.")
            .instructionText("Enter the number of most likely tokens to be returned. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("null")
            .build(),
        textProperty(SUFFIX).label("Suffix")
            .isRequired(false)
            .isExpressionable(true)
            .instructionText("The suffix that comes after a completion of inserted text.")
            .placeholder("null")
            .build(),
        booleanProperty(ECHO).label("Echo")
            .isRequired(false)
            .isExpressionable(true)
            .instructionText("Echo back the prompt in addition to the completion. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("false")
            .build(),
        listTypeProperty(STOP).label("Stop")
            .itemType(SystemType.STRING)
            .isRequired(false)
            .isExpressionable(true)
            .description("Up to four sequences where the API will stop generating further tokens. The returned text won't contain the stop sequence.")
            .instructionText("API will stop generating further tokens at this or these sequences. Follow the format:\n" +
                "{\n\t\"example sequence # 1\",\n\t\"example sequence #2\"\n }")
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
            .instructionText("Number between -2.0 and 2.0.")
            .placeholder("0.0")
            .description("Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
            .build(),
        integerProperty(BEST_OF).label("Best Of")
            .isRequired(false)
            .isExpressionable(true)
            .description("Generates best_of completions server-side and returns the \"best\" (the one with the lowest log probability per token).Because this parameter generates many completions, it can quickly consume your token quota. Use carefully and ensure that you have reasonable settings for max_tokens and stop. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("Must be greater than or equal to N. Will default to be equal to N.")
            .instructionText("Generates this many best_of completions server-side. Must be greater than or equal to N. This parameter cannot be used with gpt-35-turbo.")
            .build()
    );
  }


  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    //    1. set up step
  //    retrieve from CSP
    String apiKey = connectedSystemConfiguration.getValue(AzureOpenAICSP.API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(AzureOpenAICSP.YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);
    requestDiagnostic.put("Endpoint", endpoint);


    //    retrieve data from integration
    HashMap<String, Object> inputMap = new HashMap<>();

//    prompt
//    TODO change this?
    ArrayList<Object> prompt = integrationConfiguration.getValue(PROMPT);
    String formattedStr;
    String value;

    if (prompt != null) {
      for (int i = 0; i < prompt.size(); i++) {
        value = (String)((PropertyState)prompt.get(i)).getValue();
//        formattedStr = String.format("\"%s\"", (((PropertyState)prompt.get(i)).getValue()));
        prompt.set(i, value);
      }
    } else {
      prompt = new ArrayList<>();
      prompt.add("");
    }

    requestDiagnostic.put("Prompt", prompt);
    inputMap.put(PROMPT, prompt);

//    stop
//    TODO change this too?
    ArrayList<Object> stopArr = integrationConfiguration.getValue(STOP);
    String stopString;
    if (stopArr != null) {
      for (int i = 0; i < stopArr.size(); i++) {
//        stopString = String.format("\"%s\"", (((PropertyState)stopArr.get(i)).getValue()));
        stopString = (String)((PropertyState)prompt.get(i)).getValue();
        stopArr.set(i, stopString);
      }
    }

    inputMap.put(STOP, stopArr);
//    requestDiagnostic.put("Stop", stopArr);

//    max_tokens
    Integer max_tokens = integrationConfiguration.getValue(MAX_TOKENS);
    inputMap.put(MAX_TOKENS, max_tokens);
//    requestDiagnostic.put("Max Tokens", max_tokens);

//    temperature
    Double temperature = integrationConfiguration.getValue(TEMPERATURE);
    inputMap.put(TEMPERATURE, temperature);
//    requestDiagnostic.put("Temperature", temperature);

// top p
    Double top_p = integrationConfiguration.getValue(TOP_P);
    inputMap.put(TOP_P, top_p);
//    requestDiagnostic.put("Top P", top_p);

//    logit bias
    String logitBias = integrationConfiguration.getValue(LOGIT_BIAS);
    inputMap.put(LOGIT_BIAS, logitBias);
//    requestDiagnostic.put("Logit Bias", logitBias);

//    user
    String user = integrationConfiguration.getValue(USER);
    inputMap.put(USER, user);
//    requestDiagnostic.put("user", user);

    //      n
    Integer nInt = integrationConfiguration.getValue(N);
    inputMap.put(N, nInt);
//    requestDiagnostic.put("N", nInt);

//    log probs
    Integer logProbs = integrationConfiguration.getValue(LOGPROBS);
    inputMap.put(LOGPROBS, logProbs);
//    requestDiagnostic.put("Log Probs", logProbs);

//    suffix
    String suffix = integrationConfiguration.getValue(SUFFIX);
    inputMap.put(SUFFIX, suffix);
//    requestDiagnostic.put("Suffix", suffix);

//    Echo
    Boolean echo = integrationConfiguration.getValue(ECHO);
    if (echo == false) {
      echo = null;
    }
    inputMap.put(ECHO, echo);
//    requestDiagnostic.put("Echo", echo);

//    presence_penalty
    Double presencePenNum = integrationConfiguration.getValue(PRESENCE_PENALTY);
    inputMap.put(PRESENCE_PENALTY, presencePenNum);
//    requestDiagnostic.put("Presence Penalty", presencePenNum);

//    frequency_penalty
    Double freqPenNum= integrationConfiguration.getValue(FREQUENCY_PENALTY);
    inputMap.put(FREQUENCY_PENALTY, freqPenNum);
//    requestDiagnostic.put("Frequency Penalty", freqPenNum);

//    best_of
    Integer best_of = integrationConfiguration.getValue(BEST_OF);
    inputMap.put(BEST_OF, best_of);
//    requestDiagnostic.put("Best of", best_of);

    Map<String, Object> resultMap = new HashMap<>();
    Map<String, Object> diagnosticResponse = new HashMap<>();
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();

    //   2. make remote request
    String response = "";
    final long start = System.currentTimeMillis();
    IntegrationError error = null;
    final IntegrationDesignerDiagnostic diagnostic;

    try {
      response = completionAPICall(apiKey, endpoint, inputMap);
      resultMap.put("Response", getFullResponseObject(response));

    } catch (Exception e) {
      String[] errorDetails = getErrorDetails(response);
      error = IntegrationError.builder()
          .title("Error Title: " + errorDetails[0])
          .message("While calling the Completion endpoint, an error occurred. Please check your Deployment ID and API Version.\n")
          .detail(errorDetails[1])
          .build();
    } finally {

      diagnosticResponse.put("Full Response", response);


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
