package com.cms.enums;

public enum ParticipantRole {
    HOST("主持人"),
    ADMIN("管理员"),
    PARTICIPANT("与会者");

    private final String description;

    ParticipantRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}