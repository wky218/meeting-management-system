package com.cms.enums;
//MeetingVisibility枚举类
public enum MeetingVisibility {
    PUBLIC("公开会议"),
    SEARCHABLE("可搜索会议"),
    PRIVATE("私密会议");

    private final String description;

    MeetingVisibility(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}