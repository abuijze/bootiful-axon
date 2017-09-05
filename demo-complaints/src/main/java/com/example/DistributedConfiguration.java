package com.example;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.distributed.AnnotationRoutingStrategy;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.springcloud.commandhandling.SpringCloudCommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Optional;

@Configuration
public class DistributedConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DistributedConfiguration.class);

    @Bean
    public CommandRouter commandRouter(DiscoveryClient discoveryClient) {
        return new SpringCloudCommandRouter(discoveryClient, new AnnotationRoutingStrategy()) {
            @Override
            public Optional<Member> findDestination(CommandMessage<?> commandMessage) {
                Optional<Member> dest = super.findDestination(commandMessage);
                dest.ifPresent(d -> {
                    logger.warn("Chose destination for a {}: {} --> {}:{}", commandMessage.getCommandName(),
                                d.name(),
                                d.getConnectionEndpoint(URI.class).get().getHost(),
                                d.getConnectionEndpoint(URI.class).get().getPort()
                    );
                });
                return dest;
            }

            @Override
            public void updateMemberships(HeartbeatEvent event) {
                super.updateMemberships(event);
            }
        };

    }

}
