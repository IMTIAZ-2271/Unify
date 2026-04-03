package com.Unify.model;

import java.time.LocalDateTime;

public class JoinRequest {
    private int id;
    private int groupId;
    private String groupName;
    private int userId;
    private String username;
    private String displayName;
    private byte[] userPic;
    private String status;
    private LocalDateTime requestedAt;

    public int getId() {
        return id;
    }

    public void setId(int v) {
        this.id = v;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int v) {
        this.groupId = v;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String v) {
        this.groupName = v;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int v) {
        this.userId = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : username;
    }

    public void setDisplayName(String v) {
        this.displayName = v;
    }

    public byte[] getUserPic() {
        return userPic;
    }

    public void setUserPic(byte[] v) {
        this.userPic = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime v) {
        this.requestedAt = v;
    }
}
