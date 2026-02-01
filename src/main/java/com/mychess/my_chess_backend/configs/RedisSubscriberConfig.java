package com.mychess.my_chess_backend.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychess.my_chess_backend.services.events.RedisEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis Pub/Sub messaging.
 * Sets up message listener container for cross-server communication.
 */
@Configuration
public class RedisSubscriberConfig {
  private final ObjectMapper objectMapper;

  public RedisSubscriberConfig(
      ObjectMapper objectMapper
  ) {
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a Redis message listener container that subscribes to room move events.
   * Uses pattern subscription to listen to all room channels: room:*:moves
   */
  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisEventSubscriber subscriber
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(
        subscriber,
        new PatternTopic("room:*:moves")
    );

    return container;
  }

  /**
   * Creates a RedisTemplate configured for Pub/Sub with JSON serialization.
   * This template is used by RedisEventPublisher to send messages.
   */
  @Bean
  public RedisTemplate<String, Object> pubSubRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    template.afterPropertiesSet();
    return template;
  }
}
