package s4gh;

import java.util.HashMap;
import java.util.Map;

public class VariablesTransfer {
    private static Map<String, Object> values = new HashMap<>();

    public static Object getVariableValue(String key) {
        return values.get(key);
    }

    public static void setVariableValue(String key,  Object value) {
        values.put(key, value);
    }
}
