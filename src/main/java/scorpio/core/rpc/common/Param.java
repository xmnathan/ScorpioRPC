package scorpio.core.rpc.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 参数传递对象
 * 兼容Scorpio框架的Param
 */
public class Param {
    private final Map<String, Object> data = new HashMap<>();
    
    public Param() {
    }
    
    public Param(Object... params) {
        if (params != null) {
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    String key = params[i].toString();
                    Object value = params[i + 1];
                    data.put(key, value);
                }
            }
        }
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = (T) data.get(key);
        return value != null ? value : defaultValue;
    }
    
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    public Object remove(String key) {
        return data.remove(key);
    }
    
    public void clear() {
        data.clear();
    }
    
    public int size() {
        return data.size();
    }
    
    @Override
    public String toString() {
        return data.toString();
    }
}
