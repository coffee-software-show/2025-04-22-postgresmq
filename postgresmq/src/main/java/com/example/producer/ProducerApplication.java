package com.example.producer;

import org.postgresql.jdbc.PgConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.QueueChannelSpec;
import org.springframework.integration.jdbc.channel.PostgresChannelMessageTableSubscriber;
import org.springframework.integration.jdbc.channel.PostgresSubscribableChannel;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.time.Instant;

@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

}

@Controller
@ResponseBody
class TriggerController {

    private final MessageChannel out;

    TriggerController(@Qualifier(Channels.OUT) MessageChannel out) {
        this.out = out;
    }

    @PostMapping("/go")
    void send() {
        this.out.send(MessageBuilder.withPayload("Hello World @ " + Instant.now()).build());
    }

}


class Channels {

    static final String OUT = "out";

    static final String IN = "in";

    static final String TOPIC = "group1";
}

@Configuration
class ProducerConfiguration {

    @Bean(Channels.OUT)
    QueueChannelSpec.MessageStoreSpec out(JdbcChannelMessageStore messageStore) {
        return MessageChannels.queue(messageStore, Channels.TOPIC);
    }
}

@Configuration
class CommonConfiguration {

    @Bean
    JdbcChannelMessageStore messageStore(DataSource dataSource) {
        JdbcChannelMessageStore messageStore = new JdbcChannelMessageStore(dataSource);
        messageStore.setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
        return messageStore;
    }

}

@Configuration
class ConsumerConfiguration {


    @Bean
    PostgresChannelMessageTableSubscriber subscriber(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        return new PostgresChannelMessageTableSubscriber(() ->
                DriverManager.getConnection(url, username, password).unwrap(PgConnection.class));
    }

    @Bean(Channels.IN)
    PostgresSubscribableChannel in(
            PostgresChannelMessageTableSubscriber subscriber,
            JdbcChannelMessageStore messageStore) {
        return new PostgresSubscribableChannel(messageStore, Channels.TOPIC, subscriber);
    }

    @Bean
    IntegrationFlow integrationFlow(@Qualifier(Channels.IN) MessageChannel inbound) {
        return IntegrationFlow
                .from(inbound)
                .handle((payload, headers) -> {
                    System.out.println("payload: " + payload);
                    headers.forEach((k, v) -> System.out.println("key: " + k + " value: " + v));
                    return null;
                })
                .get();
    }
}