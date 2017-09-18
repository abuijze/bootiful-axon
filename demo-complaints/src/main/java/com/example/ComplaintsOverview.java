package com.example;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ProcessingGroup("myTracker")
@RestController
@RequestMapping("/complaints")
public class ComplaintsOverview {

    private final List<String> complaintCompanies = new ArrayList<>();

    @GetMapping("/")
    public Map<String, Long> getOverview() {
        return complaintCompanies.stream()
                                 .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @EventHandler
    public void handle(ComplaintFiledEvent event) {
        this.complaintCompanies.add(event.getCompany());
    }
}
