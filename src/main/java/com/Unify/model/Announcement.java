package com.Unify.model;

import java.sql.Timestamp;

public class Announcement {
    private int id;
    private int groupId;
    private int authorId;
    private String authorName; // Useful for displaying who posted it
    private String title;
    private String content;
    private Timestamp createdAt;

    public Announcement(int id, int groupId, int authorId, String authorName, String title, String content, Timestamp createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() { return id; }
    public int getGroupId() { return groupId; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Timestamp getCreatedAt() { return createdAt; }
}