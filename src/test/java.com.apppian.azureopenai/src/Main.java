import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class Main {




  public static void main(String[] args) {
    Map<String, Object> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");


    // Create an instance of Gson
    Gson gson = new Gson();
    // Convert map to JSON string
    String jsonString = gson.toJson(map);
    System.out.println(jsonString);


    System.out.println("Hello world!");
  }
}
