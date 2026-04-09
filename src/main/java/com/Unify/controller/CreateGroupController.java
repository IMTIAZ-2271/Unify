package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.GroupDAO;
import com.Unify.model.Group;
import com.Unify.util.AsyncWriter;
import com.Unify.util.Imgs;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.AbstractMap;

public class CreateGroupController {

    @FXML
    private Label titleLabel;
    @FXML
    private ImageView previewImg;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descArea;
    @FXML
    private ComboBox<Group> parentCombo;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveBtn;

    private Group existing;
    private byte[] picBytes;
    private Runnable onClose;
    private final GroupDAO dao = new GroupDAO();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        Imgs.circle(previewImg, 40);
        loadParents();
    }

    private void loadParents() {
        try {
            Group none = new Group();
            none.setName("None (Top-level group)");
            none.setId(0);
            parentCombo.getItems().add(none);
            parentCombo.getItems().addAll(AppData.get().getGroupsIAmAdminOf());
            parentCombo.setValue(none);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGroup(Group g) {
        this.existing = g;
        titleLabel.setText("Edit Group");
        nameField.setText(g.getName());
        descArea.setText(g.getDescription() != null ? g.getDescription() : "");
        Image img = Imgs.fromBytes(g.getProfilePicture());
        if (img != null) previewImg.setImage(img);
    }

    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    @FXML
    private void pickImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(saveBtn.getScene().getWindow());
        if (f != null) {
            AsyncWriter.get().write(
                    () -> {
                        byte[] bytes = Imgs.toBytes(f);
                        Image image = Imgs.fromBytes(bytes);
                        return new AbstractMap.SimpleEntry<>(bytes, image);
                    },
                    (result) -> Platform.runLater(() -> {
                        picBytes = result.getKey();
                        if (result.getValue() != null) previewImg.setImage(result.getValue());
                        Imgs.circle(previewImg, 40);
                        errorLabel.setVisible(false);
                    }),
                    (error) -> Platform.runLater(() -> err("Could not load image."))
            );
        }
    }

    @FXML
    private void doSave() {
        String name = nameField.getText().trim();
        String desc = descArea.getText().trim();
        byte[] pic = picBytes;
        if (name.isEmpty()) {
            err("Name is required.");
            return;
        }
        Integer parentId = null;
        Group sel = parentCombo.getValue();
        if (sel != null && sel.getId() != 0) parentId = sel.getId();
        Integer selectedParentId = parentId;
        AsyncWriter.get().write(
                () -> {
                    Group group;
                    byte[] savedPic = pic;

                    if (existing != null) {
                        dao.update(existing.getId(), name, desc, pic);
                        group = new Group();
                        group.setId(existing.getId());
                        group.setGroupCode(existing.getGroupCode());
                        group.setName(name);
                        group.setDescription(desc);
                        group.setProfilePicture(pic != null ? pic : existing.getProfilePicture());
                        group.setCreatedBy(existing.getCreatedBy());
                        group.setMemberCount(existing.getMemberCount());
                        group.setCurrentUserRole(existing.getCurrentUserRole());
                        group.setParentGroupId(existing.getParentGroupId());
                        group.setParentGroupName(existing.getParentGroupName());
                        group.setCreatedAt(existing.getCreatedAt());
                        savedPic = group.getProfilePicture();
                    } else {
                        group = dao.create(name, desc, Session.uid(), selectedParentId);
                        if (group != null && pic != null) {
                            dao.update(group.getId(), name, desc, pic);
                            group.setProfilePicture(pic);
                        }
                        if (group != null) savedPic = group.getProfilePicture();
                    }

                    Image image = Imgs.fromBytes(savedPic);
                    return new AbstractMap.SimpleEntry<>(group, image);
                },
                (result) -> Platform.runLater(() -> {
                    if (result.getValue() != null) previewImg.setImage(result.getValue());
                    Imgs.circle(previewImg, 40);
                    if (result.getKey() != null) AppData.get().addOrUpdateGroup(result.getKey());
                    Navigator.close(titleLabel, onClose);
                }),
                (error) -> Platform.runLater(() -> {
                    err("Error: " + error.getMessage());
                    error.printStackTrace();
                })
        );
    }

    @FXML
    private void doCancel() {
        //Navigator.closeModal(onClose);
        Navigator.close(titleLabel, onClose);
    }

    private void err(String m) {
        errorLabel.setText(m);
        errorLabel.setVisible(true);
    }

}
