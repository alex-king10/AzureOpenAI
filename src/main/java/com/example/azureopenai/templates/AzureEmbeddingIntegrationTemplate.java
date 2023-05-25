package com.example.azureopenai.templates;

import static com.example.azureopenai.templates.AzureOpenAICSP.API_KEY;
import static com.example.azureopenai.templates.AzureOpenAICSP.YOUR_RESOURCE_NAME;
import static std.ConstantKeys.API_VERSION;
import static std.ConstantKeys.DEPLOYMENT_ID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
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

  public static final String INTEGRATION_PROP_KEY = "intProp";


  public static final String INPUT = "input";
  private static String getFullEndpoint(String resourceName, String deploymentID, String APIVersion) {
    return String.format("https://%s.openai.azure.com/openai/deployments/%s/embeddings?api-version=%s", resourceName, deploymentID, APIVersion);
  }

  private JSONArray getEmbeddingArray(String response) {
    JSONObject jsonResponse = new JSONObject(response);
    JSONArray data = jsonResponse.getJSONArray("data");
    JSONArray embedding = new JSONArray();
    if (data.length() > 0) {
      JSONObject dataObject = data.getJSONObject(0);
      embedding = dataObject.getJSONArray("embedding");
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
    Map<String,Object> result = new HashMap<>();
    String apiKey = connectedSystemConfiguration.getValue(API_KEY);
    String resourceName = connectedSystemConfiguration.getValue(YOUR_RESOURCE_NAME);
    String deploymentID = integrationConfiguration.getValue(DEPLOYMENT_ID);
    String APIVersion = integrationConfiguration.getValue(API_VERSION);
    String endpoint = getFullEndpoint(resourceName, deploymentID, APIVersion);
    requestDiagnostic.put("API Key", apiKey);
    requestDiagnostic.put("Endpoint", endpoint);

//    retrieve from integration
    String input = "";
    input = integrationConfiguration.getValue(INPUT);
    requestDiagnostic.put("Input for embedding", input);


//   2. make remote request

    // Important for debugging to capture the amount of time it takes to interact
    // with the external system. Since this integration doesn't interact
    // with an external system, we'll just log the calculation time of concatenating the strings
    final long start = System.currentTimeMillis();

    OkHttpClient client = new OkHttpClient();
    String requestBody = String.format(
        "{\"input\": \"%s\"}", input);

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
    result.put("response", responseBody);

    final long end = System.currentTimeMillis();
    final long executionTime = end - start;
//    create diagnostics (modeled after Google Drive CS Example)
    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("Response", responseBody);
//    resultMap.put("Content", )
    IntegrationDesignerDiagnostic.IntegrationDesignerDiagnosticBuilder diagnosticBuilder = IntegrationDesignerDiagnostic
        .builder();

    Map<String, Object> diagnosticResponse = new HashMap<>();
    String embedding = getEmbeddingArray(responseBody).toString();
    diagnosticResponse.put("Embedding", embedding);
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
