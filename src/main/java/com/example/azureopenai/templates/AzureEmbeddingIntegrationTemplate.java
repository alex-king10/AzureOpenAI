package com.example.azureopenai.templates;

import static com.example.azureopenai.templates.AzureOpenAICSP.API_KEY;
import static com.example.azureopenai.templates.AzureOpenAICSP.YOUR_RESOURCE_NAME;
import static std.ConstantKeys.API_VERSION;
import static std.ConstantKeys.DEPLOYMENT_ID;
import static std.SharedMethods.getErrorDetails;

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
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Must provide an integration id. This value need only be unique for this connected system
@TemplateId(name="AzureEmbeddingIntegrationTemplate")
// Set template type to READ since this integration does not have side effects
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureEmbeddingIntegrationTemplate extends SimpleIntegrationTemplate {

  private static String embeddingsAPICall(String apiKey, String endpoint, HashMap<String, Object> inputMap)
      throws Exception {
    OkHttpClient client = new OkHttpClient();

    String requestBody = String.format(
        "{\"input\": \"%s\"}", inputMap.get("input"));

    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestBody);

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();

    Response response = null;
    String responseBody;

    response = client.newCall(request).execute();
    responseBody = response.body().string();

    response.close();
    return responseBody;


  }
  public static final String INTEGRATION_PROP_KEY = "intProp";

  public static final String INPUT = "input";
  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/embeddings?api-version=%s", resourceName, deploymentID, APIVersion);
  }

  private ArrayList<String> getEmbeddingArray(String response) {
    String responseStr = response;
    JSONObject jsonResponse = new JSONObject(responseStr);
    JSONArray data = jsonResponse.getJSONArray("data");
    ArrayList<String> embedding = new ArrayList<>();
    if (data.length() > 0) {
      for (int i = 0; i < data.length(); i++) {
        JSONObject dataObject = data.getJSONObject(i);
        embedding.add(dataObject.get("embedding").toString());

      }

    }
    return embedding;
  }
  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {


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
        textProperty(INPUT).label("Input for embeddings")
            .isRequired(true)
//            false if properties need to be hard coded and baked by the time user presses send
            .isExpressionable(true)
//            to change later when it is more than chat completion?
            .description("Input text to get embeddings for, encoded as a string.")
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
//    Map<String,Object> result = new HashMap<>();
    String apiKey = connectedSystemConfiguration.getValue(API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);
    requestDiagnostic.put("Endpoint", endpoint);

//    retrieve from integration
    String input = "";
    input = integrationConfiguration.getValue(INPUT);
    requestDiagnostic.put("Input for embedding", input);


    Map<String,Object> resultMap = new HashMap<>();
    Map<String, Object> diagnosticResponse = new HashMap<>();
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();
//   2. make remote request
    String response = "";
    final long start = System.currentTimeMillis();
    HashMap<String, Object> inputMap = new HashMap<>();
    inputMap.put("input", input);
    IntegrationError error = null;
    IntegrationDesignerDiagnostic diagnostic;


    try {
      response = embeddingsAPICall(apiKey, endpoint, inputMap);
      resultMap.put("Embeddings", getEmbeddingArray(response));


    } catch (Exception e) {
//      throw new RuntimeException(e);
      String[] errorDetails = getErrorDetails(response);
      error = IntegrationError.builder()
          .title("Error Title: " + errorDetails[0])
          .message("While calling the Embeddings endpoint, an error occurred. Please check your Deployment ID and API Version.\n")
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
