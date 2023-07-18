package std;


public interface ConstantKeys {
  String APIKEY = "apiKey";
  String ORGANIZATION = "organization";

//  Endpoint Information
  public static final String DEPLOYMENT_ID = "deployment_id";
  public static final String API_VERSION = "api_version";

  String ENDPOINT = "endpoint";



//  Configuration keys - used when retrieving info from integration config
  public static String TEMPERATURE = "temperature";
  public static String MAX_TOKENS = "max_tokens";
  public static final String LOGIT_BIAS = "logit_bias";
  public static final String USER = "user";
  public static final String N = "n";
  public static final String PRESENCE_PENALTY = "presence_penalty";
  public static final String FREQUENCY_PENALTY = "frequency_penalty";
  public static final String STOP = "stop";
  public static final String TIMEOUT = "timeout";


}
