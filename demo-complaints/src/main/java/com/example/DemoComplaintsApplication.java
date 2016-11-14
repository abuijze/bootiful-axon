package com.example;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@SpringBootApplication
public class DemoComplaintsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoComplaintsApplication.class, args);
    }

    @RequestMapping("/complaints")
    @RestController
    public static class ComplaintAPI {

        private final CommandGateway commandGateway;
        private final ComplaintQueryObjectRepository complaintsQueryObjectRepository;

        public ComplaintAPI(CommandGateway commandGateway, ComplaintQueryObjectRepository complaintsQueryObjectRepository) {
            this.commandGateway = commandGateway;
            this.complaintsQueryObjectRepository = complaintsQueryObjectRepository;
        }

        @PostMapping
        public CompletableFuture<String> fileComplaint(@RequestBody Map<String, String> request) {
            String id = UUID.randomUUID().toString();
            return commandGateway.send(new FileComplaintCommand(id, request.get("company"), request.get("description")));
        }

        @GetMapping
        public List<ComplaintQueryObject> findAll() {
            return complaintsQueryObjectRepository.findAll();
        }

        @GetMapping("/{id}")
        public ComplaintQueryObject find(@PathVariable String id) {
            return complaintsQueryObjectRepository.findOne(id);
        }
    }

    @Component
    public static class ComplaintQueryObjectUpdater {

        private final ComplaintQueryObjectRepository complaintsQueryObjectRepository;

        public ComplaintQueryObjectUpdater(ComplaintQueryObjectRepository complaintsQueryObjectRepository) {
            this.complaintsQueryObjectRepository = complaintsQueryObjectRepository;
        }

        @EventHandler
        public void on(ComplaintFiledEvent event) {
            complaintsQueryObjectRepository.save(new ComplaintQueryObject(event.getId(), event.getCompany(), event.getDescription()));
        }
    }

    @Aggregate
    public static class Complaint {

        @AggregateIdentifier
        private String complaintId;

        public Complaint() {
        }

        @CommandHandler
        public Complaint(FileComplaintCommand command) {
            Assert.hasLength(command.getCompany());
            apply(new ComplaintFiledEvent(command.getId(), command.getCompany(), command.getDescription()));
        }

        @EventSourcingHandler
        protected void on(ComplaintFiledEvent event) {
            this.complaintId = event.getId();
        }

    }

    public static class FileComplaintCommand {

        private final String id;
        private final String company;
        private final String description;

        public FileComplaintCommand(String id, String company, String description) {
            this.id = id;
            this.company = company;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getCompany() {
            return company;
        }

        public String getDescription() {
            return description;
        }
    }


    @Bean
    public Exchange exchange() {
        return ExchangeBuilder.fanoutExchange("Complaints").build();
    }

    @Bean
    public Queue queue() {
        return QueueBuilder.durable("Complaints").build();
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with("*").noargs();
    }

    @Autowired
    public void configure(AmqpAdmin admin) {
        admin.declareExchange(exchange());
        admin.declareQueue(queue());
        admin.declareBinding(binding());
    }
}
