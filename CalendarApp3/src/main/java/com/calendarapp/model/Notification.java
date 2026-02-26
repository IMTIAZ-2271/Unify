package com.calendarapp.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Integer referenceId;
    private LocalDateTime createdAt;
    private int inviteAccepted;

    public Notification() {}
    public Notification(int userId, String title, String message, String type) {
        this.userId = userId; this.title = title; this.message = message; this.type = type;
    }

    public int getId()                          { return id; }
    public void setId(int v)                    { this.id = v; }
    public int getUserId()                      { return userId; }
    public void setUserId(int v)                { this.userId = v; }
    public String getTitle()                    { return title; }
    public void setTitle(String v)              { this.title = v; }
    public String getMessage()                  { return message; }
    public void setMessage(String v)            { this.message = v; }
    public String getType()                     { return type; }
    public void setType(String v)               { this.type = v; }
    public boolean isRead()                     { return read; }
    public void setRead(boolean v)              { this.read = v; }
    public Integer getReferenceId()             { return referenceId; }
    public void setReferenceId(Integer v)       { this.referenceId = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public int getInviteAccepted()              {return inviteAccepted;}
    public void setInviteAccepted(int i)        {inviteAccepted=i;}
}
