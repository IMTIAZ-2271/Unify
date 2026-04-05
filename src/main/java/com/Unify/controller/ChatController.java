package com.Unify.controller;

import com.Unify.Session;
import com.Unify.dao.ChatDAO;
import com.Unify.dao.GroupDAO;
import com.Unify.model.ChatMessage;
import com.Unify.model.Group;
import com.Unify.util.AsyncWriter;
import com.Unify.util.Imgs;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatController {

    @FXML private TextField searchGroupField;
    @FXML private ListView<Group> groupListView;

    @FXML private VBox chatArea;
    @FXML private VBox placeholderArea;

    @FXML private ImageView chatHeaderAvatar;
    @FXML private Label chatHeaderName;
    @FXML private Label chatHeaderSub;
    @FXML private Button settingsBtn; // Changed to standard Button!

    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatList;
    @FXML private TextField chatInputField;
    @FXML private Button sendBtn;

    @FXML private HBox replyPreviewBox;
    @FXML private Label replyPreviewName;
    @FXML private Label replyPreviewText;

    private final GroupDAO groupDAO = new GroupDAO();
    private final ChatDAO chatDAO = new ChatDAO();

    private Group currentGroup = null;
    private Timeline chatPoller;
    private Timestamp lastMessageTime = new Timestamp(0);
    private ChatMessage replyingToMessage = null;

    private final ObservableList<Group> masterGroupList = FXCollections.observableArrayList();
    private FilteredList<Group> filteredGroups;

    // CACHE to hold the last message strings so scrolling doesn't lag
    private final Map<Integer, String> lastMessageCache = new HashMap<>();

    // Replace lastMessageTime with lastPollTime
    private Timestamp lastPollTime = new Timestamp(0);

    // Maps to track UI elements by Message ID so we can update them in place
    private final Map<Integer, Label> messageLabels = new HashMap<>();
    private final Map<Integer, VBox> replyBoxes = new HashMap<>();

    @FXML
    public void initialize() {
        setupGroupList();
        loadGroups();

        chatArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && chatPoller != null) {
                chatPoller.stop();
            }
        });
    }

    private void loadGroups() {
        try {
            List<Group> myGroups = groupDAO.myGroups(Session.uid());

            // Fetch the last message for each group in the background
            for(Group g : myGroups) {
                ChatMessage lastMsg = chatDAO.getLastMessage(g.getId());
                if(lastMsg != null) {
                    String prefix = (lastMsg.getSenderId() == Session.uid()) ? "You: " : lastMsg.getSenderName() + ": ";
                    lastMessageCache.put(g.getId(), prefix + lastMsg.getMessage());
                } else {
                    lastMessageCache.put(g.getId(), "No messages yet");
                }
            }

            masterGroupList.setAll(myGroups);

            filteredGroups = new FilteredList<>(masterGroupList, p -> true);
            searchGroupField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredGroups.setPredicate(group -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return group.getName().toLowerCase().contains(newVal.toLowerCase());
                });
            });

            groupListView.setItems(filteredGroups);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGroupList() {
        groupListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10));

                    ImageView av = new ImageView();
                    av.setFitWidth(45); av.setFitHeight(45); // Made slightly bigger for modern feel
                    Image img = Imgs.fromBytes(g.getProfilePicture());
                    if (img != null) av.setImage(img);
                    Imgs.circle(av, 22.5);

                    VBox info = new VBox(3);
                    info.setAlignment(Pos.CENTER_LEFT);

                    Label name = new Label(g.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1E293B;");

                    // --- THE NEW LAST MESSAGE LABEL ---
                    String msgText = lastMessageCache.getOrDefault(g.getId(), "");
                    Label lastMsg = new Label(msgText);
                    lastMsg.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
                    // Keep it from stretching out of bounds
                    lastMsg.setMaxWidth(180);
                    lastMsg.setWrapText(false);

                    info.getChildren().addAll(name, lastMsg);

                    row.getChildren().addAll(av, info);
                    setGraphic(row);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #E2E8F0; -fx-cursor: hand;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    }
                }
            }
        });

        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) openChatForGroup(newVal);
        });
    }

    private void openChatForGroup(Group group) {
        this.currentGroup = group;
        placeholderArea.setVisible(false);
        placeholderArea.setManaged(false);
        chatArea.setVisible(true);
        chatArea.setManaged(true);

        chatHeaderName.setText(group.getName());
        chatHeaderSub.setText(group.getMemberCount() + " members");
        Image headerImg = Imgs.fromBytes(group.getProfilePicture());
        if (headerImg != null) chatHeaderAvatar.setImage(headerImg);
        Imgs.circle(chatHeaderAvatar, 21);

        setupPermissionsAndMenu();

        if (chatPoller != null) chatPoller.stop();

        // --- ROBUST FIX: Reset Tracking ---
        lastPollTime = new Timestamp(0);
        messageLabels.clear();
        replyBoxes.clear();

        chatList.getChildren().clear();
        cancelReply();

        try {
            List<ChatMessage> msgs = chatDAO.getMessagesForGroup(group.getId());
            for (ChatMessage m : msgs) {
                appendMessageToUI(m);

                // Track the latest update time instead of just creation time
                if (m.getUpdatedAt() != null && m.getUpdatedAt().after(lastPollTime)) {
                    lastPollTime = m.getUpdatedAt();
                } else if (m.getCreatedAt().after(lastPollTime)) {
                    lastPollTime = m.getCreatedAt();
                }
            }
            Platform.runLater(() -> chatScroll.setVvalue(1.0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        startChatPoller();
    }
    // --- MODERN CUSTOM POPUP MENU ---
    private void setupPermissionsAndMenu() {
        try {
            String permission = groupDAO.getMessagingPermission(currentGroup.getId());
            boolean isAdminOrMod = "admin".equals(currentGroup.getCurrentUserRole()) || "moderator".equals(currentGroup.getCurrentUserRole());

            if ("ADMINS_ONLY".equals(permission) && !isAdminOrMod) {
                chatInputField.setDisable(true);
                sendBtn.setDisable(true);
                chatInputField.setPromptText("🔒 Only Admins can send messages here.");
            } else {
                chatInputField.setDisable(false);
                sendBtn.setDisable(false);
                chatInputField.setPromptText("Type a message...");
            }

            if (isAdminOrMod) {
                settingsBtn.setVisible(true);
                // Assign our custom modern popup to the button click!
                settingsBtn.setOnAction(e -> showModernSettingsPopup(permission));
            } else {
                settingsBtn.setVisible(false);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showModernSettingsPopup(String currentPermission) {
        Popup popup = new Popup();
        popup.setAutoHide(true); // Closes automatically if user clicks away

        // The sleek outer box
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        Label title = new Label("Who can send messages?");
        title.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-padding: 0 0 5 0;");

        // Option 1
        Button allBtn = new Button(currentPermission.equals("ALL") ? "●  All Members" : "○  All Members");
        allBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14; -fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-text-fill: #1E293B; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setOnAction(e -> { changePermission("ALL"); popup.hide(); });

        // Option 2
        Button adminBtn = new Button(currentPermission.equals("ADMINS_ONLY") ? "●  Only Admins" : "○  Only Admins");
        adminBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14; -fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-text-fill: #1E293B; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        adminBtn.setMaxWidth(Double.MAX_VALUE);
        adminBtn.setOnAction(e -> { changePermission("ADMINS_ONLY"); popup.hide(); });

        box.getChildren().addAll(title, allBtn, adminBtn);
        popup.getContent().add(box);

        // Calculate position to show perfectly under the three-dot button
        Bounds bounds = settingsBtn.localToScreen(settingsBtn.getBoundsInLocal());
        popup.show(settingsBtn, bounds.getMinX() - 140, bounds.getMaxY() + 5);
    }

    private void changePermission(String newPermission) {
        try {
            groupDAO.setMessagingPermission(currentGroup.getId(), newPermission);
            setupPermissionsAndMenu();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void appendMessageToUI(ChatMessage m) {
        boolean isMe = m.getSenderId() == Session.uid();
        ImageView avatar = new ImageView();
        avatar.setFitWidth(32); avatar.setFitHeight(32);
        Imgs.setAvatar(avatar, m.getSenderPic(), 32);
        HBox.setMargin(avatar, new Insets(10, isMe ? 0 : 8, 0, isMe ? 8 : 0));

        VBox bubble = new VBox(4);
        String radius = isMe ? "15 15 0 15" : "15 15 15 0";
        String bgColor = isMe ? "#DCF8C6" : "white";
        bubble.setStyle("-fx-padding: 8 12 4 12; -fx-background-radius: " + radius + "; -fx-background-color: " + bgColor + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 2, 0, 0, 1);");
        bubble.setMaxWidth(400);

        // --- Handle Reply Box ---
        if (m.getReplyToId() != null) {
            VBox quoteBox = new VBox(2);
            quoteBox.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-padding: 4 8; -fx-background-radius: 5; -fx-border-color: #00A884; -fx-border-width: 0 0 0 3;");
            Label quoteName = new Label(isMe && m.getReplyToName().equals(Session.currentUser().getUsername()) ? "You" : m.getReplyToName());
            quoteName.setStyle("-fx-font-weight: bold; -fx-font-size: 10; -fx-text-fill: #00A884;");
            Label quoteText = new Label(m.getReplyToMessage());
            quoteText.setStyle("-fx-font-size: 11; -fx-text-fill: #64748B;");
            quoteText.setWrapText(true);
            quoteText.setMaxHeight(30);
            quoteBox.getChildren().addAll(quoteName, quoteText);
            bubble.getChildren().add(quoteBox);

            // ROBUST FIX: Track the reply box in memory
            replyBoxes.put(m.getId(), quoteBox);
        }

        if (!isMe) {
            Label sender = new Label(m.getSenderName());
            sender.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #0284C7;");
            bubble.getChildren().add(sender);
        }

        // --- Handle Main Message Text ---
        Label text = new Label();

        if (m.isDeleted()) {
            text.setText("This message was deleted");
            text.setStyle("-fx-font-size: 14; -fx-text-fill: #888888; -fx-font-style: italic;");

            // Hide the quote box if this message is deleted
            if (replyBoxes.containsKey(m.getId())) {
                replyBoxes.get(m.getId()).setVisible(false);
                replyBoxes.get(m.getId()).setManaged(false);
            }
        } else {
            text.setText(m.getMessage());
            text.setStyle("-fx-font-size: 14; -fx-text-fill: #111827;");
        }
        text.setWrapText(true);

        // ROBUST FIX: Track the text label in memory
        messageLabels.put(m.getId(), text);

        String timeStr = m.getCreatedAt().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9CA3AF;");
        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BOTTOM_RIGHT);
        timeBox.setPadding(new Insets(2, 0, 0, 10));

        bubble.getChildren().addAll(text, timeBox);

        // --- Handle Context Menu (Reply & Delete) ---
        ContextMenu contextMenu = new ContextMenu();

        // Only allow Reply or Delete if the message is NOT deleted
        if (!m.isDeleted()) {
            MenuItem replyItem = new MenuItem("↩ Reply");
            replyItem.setOnAction(e -> {
                replyingToMessage = m;
                replyPreviewName.setText(isMe ? "Replying to yourself" : "Replying to " + m.getSenderName());
                replyPreviewText.setText(m.getMessage());
                replyPreviewBox.setVisible(true);
                replyPreviewBox.setManaged(true);
                chatInputField.requestFocus();
            });
            contextMenu.getItems().add(replyItem);

            if (isMe) {
                MenuItem deleteItem = new MenuItem("🗑 Delete");
                deleteItem.setOnAction(e -> {
                    AsyncWriter.get().write(
                            () -> chatDAO.deleteMessage(m.getId(), Session.uid()),
                            () -> {
                                // Instantly update the UI for the sender upon success
                                Platform.runLater(() -> {
                                    m.setDeleted(true);
                                    m.setMessage("This message was deleted");
                                    text.setText(m.getMessage());
                                    text.setStyle("-fx-font-size: 14; -fx-text-fill: #888888; -fx-font-style: italic;");

                                    if (replyBoxes.containsKey(m.getId())) {
                                        replyBoxes.get(m.getId()).setVisible(false);
                                        replyBoxes.get(m.getId()).setManaged(false);
                                    }
                                });
                            },
                            Throwable::printStackTrace
                    );
                });
                contextMenu.getItems().add(deleteItem);
            }
        }

        // Only attach context menu if there are valid options inside
        if (!contextMenu.getItems().isEmpty()) {
            bubble.setOnContextMenuRequested(e -> contextMenu.show(bubble, e.getScreenX(), e.getScreenY()));
        }

        HBox row = new HBox();
        if (isMe) {
            row.getChildren().addAll(bubble, avatar);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.getChildren().addAll(avatar, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
        }
        chatList.getChildren().add(row);
    }

    private void startChatPoller() {
        chatPoller = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            // Fetch anything created OR modified since the last poll
            List<ChatMessage> updates = chatDAO.getModifiedMessages(currentGroup.getId(), lastPollTime);

            if (!updates.isEmpty()) {
                boolean hasNewMessages = false;

                for (ChatMessage m : updates) {
                    // If the map contains the ID, it's an existing message that was updated (deleted)
                    if (messageLabels.containsKey(m.getId())) {
                        if (m.isDeleted()) {
                            Label textLbl = messageLabels.get(m.getId());
                            textLbl.setText("This message was deleted");
                            textLbl.setStyle("-fx-font-size: 14; -fx-text-fill: #888888; -fx-font-style: italic;");

                            VBox quoteBox = replyBoxes.get(m.getId());
                            if (quoteBox != null) {
                                quoteBox.setVisible(false);
                                quoteBox.setManaged(false);
                            }
                        }
                    } else {
                        // If it's not in the map, it's a brand new message
                        appendMessageToUI(m);
                        hasNewMessages = true;
                    }

                    // Push the poll time forward
                    if (m.getUpdatedAt().after(lastPollTime)) {
                        lastPollTime = m.getUpdatedAt();
                    }
                }

                // Only auto-scroll if an actual new message was added
                if (hasNewMessages) {
                    Platform.runLater(() -> chatScroll.setVvalue(1.0));

                    // Update sidebar cache
                    ChatMessage last = updates.get(updates.size() - 1);
                    String prefix = (last.getSenderId() == Session.uid()) ? "You: " : last.getSenderName() + ": ";
                    lastMessageCache.put(currentGroup.getId(), prefix + last.getMessage());
                    groupListView.refresh();
                }
            }
        }));
        chatPoller.setCycleCount(Timeline.INDEFINITE);
        chatPoller.play();
    }
    @FXML
    private void cancelReply() {
        replyingToMessage = null;
        replyPreviewBox.setVisible(false);
        replyPreviewBox.setManaged(false);
    }

    @FXML
    private void sendMessage() {
        String msg = chatInputField.getText().trim();
        if (msg.isEmpty() || currentGroup == null) return;

        chatInputField.clear();
        Integer replyId = replyingToMessage != null ? replyingToMessage.getId() : null;
        cancelReply();

        AsyncWriter.get().write(
                () -> chatDAO.sendMessage(currentGroup.getId(), Session.uid(), msg, replyId),
                () -> {},
                Throwable::printStackTrace
        );
    }
}