package std;

import org.json.JSONObject;

public class SharedMethods {
  public static String[] getErrorDetails(String responseBdoy) {
    JSONObject responseJSON = new JSONObject(responseBdoy);
    String errorTitle = (String)((JSONObject)responseJSON.get("error")).get("code");
    String message = (String)((JSONObject)responseJSON.get("error")).get("message");
    return new String[] {errorTitle, message};
  }

}
