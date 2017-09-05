package com.example;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.CommandGatewayFactory;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.messaging.correlation.MessageOriginProvider;
import org.axonframework.messaging.correlation.MultiCorrelationDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static java.util.Collections.singletonMap;

@Configuration
public class CorrelationConfiguration {

    @Value("${CF_INSTANCE_INDEX:${CF_INSTANCE_IP:Unkown}}")
    private String nodeId;

    @Bean
    public CorrelationDataProvider originCorrelationDataProvider() {
        return new MultiCorrelationDataProvider<>(Arrays.<CorrelationDataProvider>asList(
                new MessageOriginProvider(),
                message -> singletonMap("command-origin", message.getMetaData().get("originAddress")),
                message -> singletonMap("processing-node", nodeId)
        ));
    }

    @Bean
    public CommandGateway commandGateway(CommandBus commandBus) {
        return new CommandGatewayFactory(commandBus)
                .registerDispatchInterceptor(messages -> (i, c) -> c.andMetaData(singletonMap("originAddress", nodeId)))
                .createGateway(CommandGateway.class);
    }

}
