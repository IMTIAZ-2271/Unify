package com.calendarapp.model;

import java.time.LocalDateTime;

public class Event {
    private int id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String eventType; // "personal" or "group"
    private Integer groupId;
    private String groupName;
    private int createdBy;
    private String createdByUsername;
    private String color;
    private boolean allDay;
    private LocalDateTime createdAt;

    public Event() { this.color = "#3B82F6"; this.eventType = "personal"; }

    public int getId()                          { return id; }
    public void setId(int v)                    { this.id = v; }
    public String getTitle()                    { return title; }
    public void setTitle(String v)              { this.title = v; }
    public String getDescription()              { return description; }
    public void setDescription(String v)        { this.description = v; }
    public String getLocation()                 { return location; }
    public void setLocation(String v)           { this.location = v; }
    public LocalDateTime getStartTime()         { return startTime; }
    public void setStartTime(LocalDateTime v)   { this.startTime = v; }
    public LocalDateTime getEndTime()           { return endTime; }
    public void setEndTime(LocalDateTime v)     { this.endTime = v; }
    public String getEventType()                { return eventType; }
    public void setEventType(String v)          { this.eventType = v; }
    public Integer getGroupId()                 { return groupId; }
    public void setGroupId(Integer v)           { this.groupId = v; }
    public String getGroupName()                { return groupName; }
    public void setGroupName(String v)          { this.groupName = v; }
    public int getCreatedBy()                   { return createdBy; }
    public void setCreatedBy(int v)             { this.createdBy = v; }
    public String getCreatedByUsername()        { return createdByUsername; }
    public void setCreatedByUsername(String v)  { this.createdByUsername = v; }
    public String getColor()                    { return color; }
    public void setColor(String v)              { this.color = v; }
    public boolean isAllDay()                   { return allDay; }
    public void setAllDay(boolean v)            { this.allDay = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    public boolean isPersonal() { return "personal".equals(eventType); }
    public boolean isGroup()    { return "group".equals(eventType); }
}
