package com.Unify.model;

import java.sql.Timestamp;

public class ChatMessage {
    private int id;
    private int groupId;
    private int senderId;
    private String senderName; // To display the sender's name in chat
    private String message;
    private Timestamp createdAt;
    private byte[] senderPic;

    private Integer replyToId;
    private String replyToName;
    private String replyToMessage;

    public ChatMessage(int id, int groupId, int senderId, String senderName, byte[] senderPic,
                       String message, Timestamp createdAt, Integer replyToId, String replyToName, String replyToMessage) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPic = senderPic;
        this.message = message;
        this.createdAt = createdAt;
        this.replyToId = replyToId;
        this.replyToName = replyToName;
        this.replyToMessage = replyToMessage;
    }

    public int getId() { return id; }
    public int getGroupId() { return groupId; }
    public int getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public byte[] getSenderPic() { return senderPic; }
    public String getMessage() { return message; }
    public Timestamp getCreatedAt() { return createdAt; }

    // NEW Getters
    public Integer getReplyToId() { return replyToId; }
    public String getReplyToName() { return replyToName; }
    public String getReplyToMessage() { return replyToMessage; }
}