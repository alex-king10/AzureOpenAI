package com.appian.azureopenai.templates;

import static std.ConstantKeys.API_VERSION;
import static std.ConstantKeys.DEPLOYMENT_ID;
import static std.SharedMethods.getErrorDetails;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.DisplayHint;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
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

@TemplateId(name="AzureEmbeddingIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class AzureEmbeddingIntegrationTemplate extends SimpleIntegrationTemplate {
  static Map<String,Object> requestDiagnostic = new HashMap<>();
  static Gson gson = new Gson();
  private static String embeddingsAPICall(String apiKey, String endpoint, HashMap<String, Object> inputMap) {
    OkHttpClient client = new OkHttpClient();
    HashMap<String, Object> requestMap = new HashMap<>();

    Object value = inputMap.get(INPUT);
    if (value != null) {
      requestMap.put(INPUT, value);
      requestDiagnostic.put(INPUT, value);
    }

    String requestBody = gson.toJson(requestMap);

    MediaType mediaType = MediaType.parse("application/json");

    RequestBody body = RequestBody.create(mediaType, requestBody);

    Request request = new Request.Builder()
        .url(endpoint)
        .method("POST", body)
        .addHeader("api-key", apiKey)
        .addHeader("Content-Type","application/json")
        .build();

    String responseBody;
    try (Response response = client.newCall(request).execute() ) {
      responseBody = response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return responseBody;


  }

  public static final String INPUT = "input";
  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/embeddings?api-version=%s", resourceName, deploymentID, APIVersion);
  }

  private Map<String, Object> getEmbeddingArray(String response) {
//    JSONObject jsonResponse = new JSONObject(response);
//    JSONArray data = jsonResponse.getJSONArray("data");
//    ArrayList<String> embedding = new ArrayList<>();
////    JSONArray jsonArray = new JSONArray(data.getJSONObject(0).get("embedding").toString());
//
//
//    if (data.length() > 0) {
//      for (int i = 0; i < data.length(); i++) {
//        JSONObject dataObject = data.getJSONObject(i);
//        JSONArray jsonArray = new JSONArray(dataObject.get("embedding").toString());
////        embedding.add(dataObject.get("embedding").toString());
//      }
//
//    }
//    return embedding;
    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, Object>>(){}.getType();

    Map<String, Object> myMap = gson.fromJson(response, type);
    return myMap;
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
            .placeholder("ex: ADA_002_DeploymentID")
            .isRequired(true)
            .isExpressionable(true)
            .instructionText("Enter the ID of the model you have deployed for use with the Embeddings endpoint.")
            .build(),
        textProperty(API_VERSION).label("API Version")
            .description("The API version to use for this operation. This follows the YYYY-MM-DD format.")
            .instructionText("API version to use for this operation.")
            .isRequired(true)
            .isExpressionable(true)
            .placeholder("ex: 2023-05-15")
            .build(),
        textProperty(INPUT).label("Input for embeddings")
            .isRequired(true)
            .isExpressionable(true)
            .instructionText("Text to generate embeddings for, encoded as a string.")
            .displayHint(DisplayHint.NORMAL)
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


    Map<String,Object> resultMap = new HashMap<>();
    Map<String, Object> diagnosticResponse = new HashMap<>();
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();
    HashMap<String, Object> inputMap = new HashMap<>();

//   2. make remote request

    //    retrieve from integration
    String input;
    input = integrationConfiguration.getValue(INPUT);
    inputMap.put(INPUT, input);

    String response = "";

    final long start = System.currentTimeMillis();
    IntegrationError error = null;
    IntegrationDesignerDiagnostic diagnostic;


    try {
      response = embeddingsAPICall(apiKey, endpoint, inputMap);

      resultMap.put("Embeddings", getEmbeddingArray(response));


    } catch (Exception e) {
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
