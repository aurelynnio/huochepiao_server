package codex.mmxxvi.service;

import java.util.concurrent.TimeUnit;

public interface CachingService {
    String getString(String key);

    void setString(String key, String value, Long timeout, TimeUnit timeUnit);

    <T> T getObject(String key, Class<T> clazz);

    void setObject(String key, Object value, Long timeout, TimeUnit timeUnit);

    void delete(String key);

    boolean checkExist(String key);
}
