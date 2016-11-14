package com.example;

public class ComplaintFiledEvent {
    private final String id;
    private final String company;
    private final String description;

    public ComplaintFiledEvent(String id, String company, String description) {
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
