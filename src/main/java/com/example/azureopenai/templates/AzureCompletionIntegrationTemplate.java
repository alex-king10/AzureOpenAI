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
import static std.ConstantKeys.STOP;
import static std.ConstantKeys.TEMPERATURE;
import static std.ConstantKeys.USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Must provide an integration id. This value need only be unique for this connected system
@TemplateId(name="AzureCompletionIntegrationTemplate")
// Set template type to READ since this integration does not have side effects
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureCompletionIntegrationTemplate extends SimpleIntegrationTemplate {


  public static final String PROMPT = "prompt";
  public static final String DEV_SETTINGS = "devSettings";
  public static final String TOP_P = "top_p";
  public static final String LOGPROBS = "logprobs";
  public static final String SUFFIX = "suffix";
  public static final String ECHO = "echo";
  public static final String BEST_OF = "best_of";
  public static final String GPT35TURBO = "gpt_35_turbo";


private static String getChoices(String responseBody) {
  JSONObject jsonResponse = new JSONObject(responseBody);
  JSONArray choices = jsonResponse.getJSONArray("choices");
  return choices.toString();
}

  private static String completionAPICall(String apiKey, String endpoint, HashMap<String, Object> inputMap) {

    String prompt = (String)(inputMap.get(PROMPT).toString());
    int max_tokens = (Integer)inputMap.get(MAX_TOKENS);
    Double temperature = (Double)inputMap.get(TEMPERATURE);
    Double top_p = (Double)inputMap.get(TOP_P);
    Boolean gpt35turbo = (Boolean)inputMap.get(GPT35TURBO);

//    might need to manipulate array to get Str
    String stop;
    if (inputMap.get(STOP) != null) {stop = (String)(inputMap.get(STOP).toString());}
    else { stop = null; }
    String logit_bias = (String)inputMap.get(LOGIT_BIAS);
    String user = (String)inputMap.get(USER);
    int n = (Integer)inputMap.get(N);
    Double presence_pen = (Double)inputMap.get(PRESENCE_PENALTY);
    Double freq_pen = (Double)inputMap.get(FREQUENCY_PENALTY);
    Integer best_of = (Integer)inputMap.get(BEST_OF);
    Integer logprobs = (Integer)inputMap.get(LOGPROBS);
    Boolean echo = (Boolean)inputMap.get(ECHO);




    OkHttpClient client = new OkHttpClient();
    String requestBody;

    if (!gpt35turbo) {
      requestBody = String.format("{\"prompt\": %s, \"stop\": %s, \"max_tokens\":%d, \"top_p\": %f," +
        "\"logit_bias\": %s, \"user\": \"%s\", \"n\": %d, \"presence_penalty\": %f," +
        "\"frequency_penalty\": %f, \"best_of\": %d, \"logprobs\": %d, \"echo\": %s," +
        "\"temperature\": %f}",
        prompt, stop, max_tokens, top_p, logit_bias,
        user, n,presence_pen, freq_pen, best_of, logprobs, echo, temperature );
    } else {
      requestBody = String.format("{\"prompt\": %s, \"stop\": %s, \"max_tokens\":%d, \"top_p\": %f," +
              "\"logit_bias\": %s, \"user\": \"%s\", \"n\": %d, \"presence_penalty\": %f," +
              "\"frequency_penalty\": %f, " +
              "\"temperature\": %f}",
          prompt, stop, max_tokens, top_p, logit_bias,
          user, n,presence_pen, freq_pen, temperature );
    }

    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestBody);

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();

    Response response = null;
    String responseBody = "";
    try {
      response = client.newCall(request).execute();
      responseBody = response.body().string();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    response.close();
//    result.put("response", responseBody);

    return responseBody;
  }


  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/completions?api-version=%s", resourceName, deploymentID, APIVersion);
  }

//  private static ArrayList<String> getCompletionResponse()
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
          listTypeProperty(PROMPT).label("Prompt")
              .itemType(SystemType.STRING)
              .isRequired(false)
              //            false if properties need to be hard coded and baked by the time user presses send
              .isExpressionable(true)
              //            to change later when it is more than chat completion?
              .description("The prompt(s) to generate completions for, encoded as a list of strings. Default is the beginning of a new document.")
              .build(),
          booleanProperty(GPT35TURBO).label("Model is GPT-35-Turbo?")
              .displayMode(BooleanDisplayMode.RADIO_BUTTON)
              .isExpressionable(true)
              .description("Using GPT-35-Turbo for your deployment model means there are some configuration options that will be unavailable such as logprobs, best_of, and echo.")
              .build(),
          booleanProperty(DEV_SETTINGS).label("Developer Settings")
              .displayMode(BooleanDisplayMode.CHECKBOX)
              .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
              .refresh(RefreshPolicy.ALWAYS)
              .build()
      );
    }

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
        listTypeProperty(PROMPT).label("Prompt")
            .itemType(SystemType.STRING)
            .isRequired(false)
            //            false if properties need to be hard coded and baked by the time user presses send
            .isExpressionable(true)
            //            to change later when it is more than chat completion?
            .description("The prompt(s) to generate completions for, encoded as a list of strings. Default is the beginning of a new document.")
            .build(),
        booleanProperty(GPT35TURBO).label("Model is GPT-35-Turbo?")
            .displayMode(BooleanDisplayMode.RADIO_BUTTON)
            .description("Using GPT-35-Turbo for your deployment model means there are some configuration options that will be unavailable such as logprobs, best_of, and echo.")
            .isExpressionable(true)
            .build(),
        booleanProperty(DEV_SETTINGS).label("Developer Settings")
            .displayMode(BooleanDisplayMode.CHECKBOX)
            .description("Check this box if you would like to set more advanced configurations for your API call. The placeholder values in each field below are the default values. If no value is given, this default value will be used.")
            .refresh(RefreshPolicy.ALWAYS)
            .build(),
        integerProperty(MAX_TOKENS).label("Max Tokens")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("16")
            .description("The maximum number of tokens to generate in the completion. The token count of your prompt plus max_tokens can't exceed the model's context length. Default of 16.")
            .build(),
        textProperty(TEMPERATURE).label("Temperature")
            .isRequired(false)
            .isExpressionable(true)
            .placeholder("1.0")
            .description("What sampling temperature to use, between 0 and 2. Higher values means the model will take more risks. Default of 1.")
            .build(),
        textProperty(TOP_P).label("Top P")
            .isRequired(false)
            .isExpressionable(true)
            .description("An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. We generally recommend altering this or temperature but not both.")
            .placeholder("1.0")
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
            .build(),
        integerProperty(N).label("n")
            .isRequired(false)
            .isExpressionable(true)
            .description("How many chat completion choices to generate for each input message. Default of 1.")
            .placeholder("1")
            .build(),
        integerProperty(LOGPROBS).label("Log Probs")
            .isRequired(false)
            .isExpressionable(true)
            .description("Include the log probabilities on the logprobs most likely tokens, as well the chosen tokens. For example, if logprobs is 10, the API will return a list of the 10 most likely tokens. the API will always return the logprob of the sampled token, so there may be up to logprobs+1 elements in the response. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("null")
            .build(),
        textProperty(SUFFIX).label("Suffix")
            .isRequired(false)
            .isExpressionable(true)
            .description("The suffix that comes after a completion of inserted text.")
            .placeholder("null")
            .build(),
        booleanProperty(ECHO).label("Echo")
            .isRequired(false)
            .isExpressionable(true)
            .description("Echo back the prompt in addition to the completion. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("false")
            .build(),
        listTypeProperty(STOP).label("Stop")
            .itemType(SystemType.STRING)
            .isRequired(false)
            .isExpressionable(true)
            .description("Up to four sequences where the API will stop generating further tokens. The returned text won't contain the stop sequence.")
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
        integerProperty(BEST_OF).label("Best Of")
            .isRequired(false)
            .isExpressionable(true)
            .description("Generates best_of completions server-side and returns the \"best\" (the one with the lowest log probability per token).Because this parameter generates many completions, it can quickly consume your token quota. Use carefully and ensure that you have reasonable settings for max_tokens and stop. This parameter cannot be used with gpt-35-turbo.")
            .placeholder("Must be greater than or equal to N. Will default to be equal to N.")
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
    Map<String,Object> requestDiagnostic = new HashMap<>();
    Map<String,Object> result = new HashMap<>();
    String apiKey = connectedSystemConfiguration.getValue(API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);
    requestDiagnostic.put("Endpoint", endpoint);


    //    retrieve data from integration
    HashMap<String, Object> inputMap = new HashMap<>();

    //    gpt-35-turbo
    Boolean gpt35Turbo = integrationConfiguration.getValue(GPT35TURBO);
    if (gpt35Turbo == null) gpt35Turbo = true;
    inputMap.put(GPT35TURBO, gpt35Turbo);

//    prompt
    ArrayList<Object> prompt = integrationConfiguration.getValue(PROMPT);
    ArrayList<String> promptArr = new ArrayList<>();
    String formattedStr = "";

    if (prompt != null) {
      for (int i = 0; i < prompt.size(); i++) {
        formattedStr = String.format("\"%s\"", ((String)((PropertyState)prompt.get(i)).getValue()));
        prompt.set(i, formattedStr);
      }
    } else {
      prompt = new ArrayList<>();
      prompt.add("");
    }

    requestDiagnostic.put("Prompt", prompt);
    inputMap.put(PROMPT, prompt);

//    stop
    ArrayList<Object> stopArr = integrationConfiguration.getValue(STOP);
    String stopString;
    if (stopArr != null) {
      for (int i = 0; i < stopArr.size(); i++) {
        stopString = String.format("\"%s\"", ((String)((PropertyState)stopArr.get(i)).getValue()));
        stopArr.set(i, stopString);
      }
    }

    inputMap.put(STOP, stopArr);
    requestDiagnostic.put("Stop", stopArr);

//    max_tokens
//      edge cases: no value entered, too high of a value, negative value, decimal
    Integer max_tokens = integrationConfiguration.getValue(MAX_TOKENS);
    if(max_tokens == null) max_tokens = 16;
    if (max_tokens < 4096) inputMap.put(MAX_TOKENS, max_tokens);
    requestDiagnostic.put("Max Tokens", max_tokens);

//    temperature
    String tempString = integrationConfiguration.getValue(TEMPERATURE);
    Double temperature = 1.0;
    if (tempString != null) { temperature = Double.valueOf(tempString); }
    if (temperature < 0.0 || temperature > 2.0) { temperature = 1.0; }
    inputMap.put(TEMPERATURE, temperature);
    requestDiagnostic.put("Temperature", temperature);

// top p
//    edge cases: less than or equal to 1, no neg.
    String topPStr = integrationConfiguration.getValue(TOP_P);
    Double top_p = 1.0;
    if (topPStr != null) { top_p = Double.valueOf(topPStr); }
    if (top_p < 0.0 || top_p > 1.0) {top_p = 1.0;}
    inputMap.put(TOP_P, top_p);
    requestDiagnostic.put("Top P", top_p);

//    logit bias
    String logitBias = integrationConfiguration.getValue(LOGIT_BIAS);
    if (logitBias == null) logitBias = new JSONObject().toString();
    inputMap.put(LOGIT_BIAS, logitBias);
    requestDiagnostic.put("Logit Bias", logitBias);

//    user
    String user = integrationConfiguration.getValue(USER);
    if (user == null) user = "";
    inputMap.put(USER, user);
    requestDiagnostic.put("user", user);

    //      n
    Integer nInt = integrationConfiguration.getValue(N);
    if(nInt == null) nInt = 1;
    inputMap.put(N, nInt);
    requestDiagnostic.put("N", nInt);

//    log probs
//    need a new deployment, don't include yet
    Integer logProbs = integrationConfiguration.getValue(LOGPROBS);
    inputMap.put(LOGPROBS, logProbs);
    requestDiagnostic.put("Log Probs", logProbs);

//    suffix
//      if nothing entered, passes in null
    String suffix = integrationConfiguration.getValue(SUFFIX);
    inputMap.put(SUFFIX, suffix);
    requestDiagnostic.put("Suffix", suffix);

//can't have suffix and echo at same time - where should I account for that?

//    Echo - needs a new deployment
    Boolean echo = integrationConfiguration.getValue(ECHO);
    inputMap.put(ECHO, echo);
    requestDiagnostic.put("Echo", echo);

//    presence_penalty
    String presencePen = integrationConfiguration.getValue(PRESENCE_PENALTY);
    double presencePenNum = 0.0;
    //    if value was given convert to double
    if(presencePen != null) presencePenNum = Double.valueOf(presencePen);
    //    if value not between -2 and 2, set to default
    if (presencePenNum > 2.0 || presencePenNum < -2.0) {
      presencePenNum = 0.0;
    }
    inputMap.put(PRESENCE_PENALTY, presencePenNum);
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
    inputMap.put(FREQUENCY_PENALTY, freqPenNum);
    requestDiagnostic.put("Frequency Penalty", freqPenNum);

//    best_of
    Integer best_of = integrationConfiguration.getValue(BEST_OF);
    if (best_of == null || best_of < nInt) best_of = nInt;
    inputMap.put(BEST_OF, best_of);
    requestDiagnostic.put("Best of", best_of);


    //   2. make remote request

    final long start = System.currentTimeMillis();

    String responseHelper = completionAPICall(apiKey, endpoint, inputMap);
//    System.out.println(responseHelper);
//
//    OkHttpClient client = new OkHttpClient();
//    String requestBody = String.format(
//        "{\"prompt\": %s}", prompt);
//
//    MediaType mediaType = MediaType.parse("application/json");
//
//    RequestBody body = RequestBody.create(mediaType, requestBody);
//
//    Request request = new Request.Builder()
//        .url(endpoint)
//        .method("POST", body)
//        .addHeader("api-key", apiKey)
//        .addHeader("Content-Type","application/json")
//        .build();
//
//    Response response = null;
//    String responseBody = "";
//    try {
//      response = client.newCall(request).execute();
//      responseBody = response.body().string();
//
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//    response.close();

    result.put("response", responseHelper);
//    result.put("response", responseBody);

    final long end = System.currentTimeMillis();
    final long executionTime = end - start;
    //    create diagnostics (modeled after Google Drive CS Example)
    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("Response", responseHelper);
//    resultMap.put("Response", responseBody);
    //    resultMap.put("Content", )
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();

    Map<String, Object> diagnosticResponse = new HashMap<>();
//    String embedding = getEmbeddingArray(responseBody).toString();
//    diagnosticResponse.put("Choices", getChoices(responseHelper));

    IntegrationDesignerDiagnostic diagnostic = diagnosticBuilder
        .addExecutionTimeDiagnostic(executionTime)
        .addRequestDiagnostic(requestDiagnostic)
        .addResponseDiagnostic(diagnosticResponse)
        .build();


    return IntegrationResponse
        .forSuccess(resultMap)
        .withDiagnostic(diagnostic)
        .build();
  }
}
