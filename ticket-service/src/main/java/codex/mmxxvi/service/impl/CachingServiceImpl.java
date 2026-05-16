package codex.mmxxvi.service.impl;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.service.CachingService;

@Service
public class CachingServiceImpl implements CachingService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public CachingServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void setString(String key, String value, Long timeout, TimeUnit timeUnit) {
        if (timeout == null || timeout <= 0 || timeUnit == null) {
            stringRedisTemplate.opsForValue().set(key, value);
            return;
        }
        stringRedisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (!clazz.isInstance(value)) {
            throw new AppExceptions.InternalServerErrorException("Cached value type mismatch for key: " + key);
        }
        return clazz.cast(value);
    }

    @Override
    public void setObject(String key, Object value, Long timeout, TimeUnit timeUnit) {
        if (timeout == null || timeout <= 0 || timeUnit == null) {
            redisTemplate.opsForValue().set(key, value);
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
        redisTemplate.delete(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        Set<String> stringKeys = stringRedisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(stringKeys)) {
            stringRedisTemplate.delete(stringKeys);
        }

        Set<String> objectKeys = redisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(objectKeys)) {
            redisTemplate.delete(objectKeys);
        }
    }

    @Override
    public boolean checkExist(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))
                || Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
