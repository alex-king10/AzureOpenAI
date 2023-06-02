import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Main {

  @Test
  public void testEmbeddingRetreival() {
    String json = "{ \"key1\": \"value1\", \"key2\": { \"key3\": \"value3\" } }";

    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, Object>>(){}.getType();

    Map<String, Object> myMap = gson.fromJson(json, type);
    System.out.println(myMap);

  }


  public static void main(String[] args) {
    ArrayList<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");

    list.add(map);

    map = new HashMap<>();
    map.put("key3", "value2");
    map.put("key4", "value2");

    list.add(map);
    // Create an instance of Gson
    Gson gson = new Gson();
    // Convert map to JSON string
    String jsonString = gson.toJson(map);
    String listStr = gson.toJson(list);
    System.out.println(jsonString);
    System.out.println(listStr);


    System.out.println("Hello world!");
  }
}
