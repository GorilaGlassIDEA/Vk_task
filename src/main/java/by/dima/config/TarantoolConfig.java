package by.dima.config;

import io.tarantool.client.TarantoolClient;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TarantoolConfig {

    @Bean
    public TarantoolClient tarantoolClient() {
        try {
            return new TarantoolCrudClientBuilder().withConnectTimeout(5000).withUser("guest").withPassword(null).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Tarantool client", e);
        }
    }
}
