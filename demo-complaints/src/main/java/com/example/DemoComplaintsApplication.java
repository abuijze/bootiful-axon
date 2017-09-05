package com.example;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.TargetAggregateIdentifier;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@EnableDiscoveryClient
@SpringBootApplication
public class DemoComplaintsApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoComplaintsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoComplaintsApplication.class, args);
    }

    @RequestMapping("/complaints")
    @RestController
    public static class ComplaintAPI {

        private static final Logger logger = LoggerFactory.getLogger(ComplaintAPI.class);
        private final CommandGateway commandGateway;
        private final ComplaintQueryObjectRepository complaintsQueryObjectRepository;
        @Value("${CF_INSTANCE_INDEX:Unknown}")
        private String instanceIndex;

        public ComplaintAPI(CommandGateway commandGateway, ComplaintQueryObjectRepository complaintsQueryObjectRepository) {
            this.commandGateway = commandGateway;
            this.complaintsQueryObjectRepository = complaintsQueryObjectRepository;
        }

        @PostMapping
        public CompletableFuture<String> fileComplaint(@RequestBody Map<String, String> request) {
            String id = UUID.randomUUID().toString();
            logger.info("Sending command for a complaint about {} from node {}",
                        request.get("company"),
                        instanceIndex);
            return commandGateway.send(new FileComplaintCommand(id, request.get("company"), request.get("description")));
        }

        @PostMapping("/{complaintId}")
        public CompletableFuture<Void> addNote(@PathVariable String complaintId, @RequestBody String note) {
            logger.info("Sending command for a note on a complaint about {} from node {}",
                        complaintId,
                        instanceIndex);
            return commandGateway.send(new AddNoteCommand(complaintId, note));
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
        public void on(ComplaintFiledEvent event, @MetaDataValue("originAddress") String origin) {
            logger.info("Received event with origin {}", origin);
            complaintsQueryObjectRepository.save(new ComplaintQueryObject(event.getId(), event.getCompany(), event.getDescription()));
        }

    }

    @Component
    public static class LoggingHandler {

        @EventHandler
        public void on(EventMessage<?> event, MetaData metaData) {
            logger.info("Received event {}: {}",
                        event.getPayloadType().getSimpleName(),
                        metaData.entrySet().stream().reduce("", (s, e) -> s + "," + e.getKey() + "=" + e.getValue(), (s1, s2) -> s1 + "," + s2));
        }

    }

    @Aggregate
    public static class Complaint {

        private static final Logger logger = LoggerFactory.getLogger(Complaint.class);
        @AggregateIdentifier
        private String complaintId;

        public Complaint() {
        }

        @CommandHandler
        public Complaint(FileComplaintCommand command) {
            Assert.hasLength(command.getCompany());
            logger.info("Received command for a complaint about: {}", command.getCompany());
            apply(new ComplaintFiledEvent(command.getId(), command.getCompany(), command.getDescription()));
        }

        @CommandHandler
        public void addNote(AddNoteCommand command) {
            logger.info("Received command for a note complaint about: {}", command.getId());
            apply(new NoteAddedEvent(command.getId(), command.getNote()));
        }

        @EventSourcingHandler
        protected void on(ComplaintFiledEvent event) {
            this.complaintId = event.getId();
        }

    }

    public static class NoteAddedEvent {
        private final String id;
        private final String note;

        public NoteAddedEvent(String id, String note) {
            this.id = id;
            this.note = note;
        }

        public String getId() {
            return id;
        }
    }

    public static class FileComplaintCommand {

        @TargetAggregateIdentifier
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

    public static class AddNoteCommand {

        @TargetAggregateIdentifier
        private final String id;
        private final String note;

        public AddNoteCommand(String id, String note) {
            this.id = id;
            this.note = note;
        }

        public String getId() {
            return id;
        }

        public String getNote() {
            return note;
        }
    }
}
