package com.Unify.model;

import java.time.LocalDateTime;

public class Group {
    private int id;
    private String groupCode;
    private String name;
    private String description;
    private byte[] profilePicture;
    private Integer parentGroupId;
    private String parentGroupName;
    private int createdBy;
    private LocalDateTime createdAt;
    private int memberCount;
    private String currentUserRole; // "admin", "member", or null

    public Group() {
    }

    public int getId() {
        return id;
    }

    public void setId(int v) {
        this.id = v;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String v) {
        this.groupCode = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] v) {
        this.profilePicture = v;
    }

    public Integer getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(Integer v) {
        this.parentGroupId = v;
    }

    public String getParentGroupName() {
        return parentGroupName;
    }

    public void setParentGroupName(String v) {
        this.parentGroupName = v;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int v) {
        this.createdBy = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int v) {
        this.memberCount = v;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String v) {
        this.currentUserRole = v;
    }

    public boolean isAdmin() {
        return "admin".equals(currentUserRole);
    }

    public boolean isMember() {
        return currentUserRole != null;
    }

    public boolean isModerator() {
        return "moderator".equals(currentUserRole);
    }

    @Override
    public String toString() {
        return name + " [" + groupCode + "]";
    }
}
