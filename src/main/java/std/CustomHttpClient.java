package std;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class CustomHttpClient {
  public static void main(String[] args) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    // Set connection timeout
    builder.connectTimeout(10, TimeUnit.SECONDS); // 10 seconds

    // Set read timeout
    builder.readTimeout(30, TimeUnit.SECONDS); // 30 seconds

    // Set write timeout
    builder.writeTimeout(30, TimeUnit.SECONDS); // 30 seconds

    OkHttpClient client = builder.build();

    // Use the custom client for your API requests
    // ...
  }
}
