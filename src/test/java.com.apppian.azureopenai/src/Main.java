import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class Main {



  public static void main(String[] args) {
    ArrayList<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");

    list.add(map);
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
