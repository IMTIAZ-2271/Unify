package com.calendarapp.controller;

import com.calendarapp.App;
import com.calendarapp.AppData;
import com.calendarapp.Navigator;
import com.calendarapp.Session;
import com.calendarapp.dao.GroupDAO;
import com.calendarapp.model.Event;
import com.calendarapp.model.Group;
import com.calendarapp.util.Imgs;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class CreateGroupController {

    @FXML private Label     titleLabel;
    @FXML private ImageView previewImg;
    @FXML private TextField nameField;
    @FXML private TextArea  descArea;
    @FXML private ComboBox<Group> parentCombo;
    @FXML private Label     errorLabel;
    @FXML private Button    saveBtn;

    private Group    existing;
    private byte[]   picBytes;
    private Runnable onClose;
    private final GroupDAO dao = new GroupDAO();

    @FXML private void initialize() {
        errorLabel.setVisible(false);
        Imgs.circle(previewImg, 40);
        loadParents();
    }

    private void loadParents() {
        try {
            Group none = new Group(); none.setName("None (Top-level group)"); none.setId(0);
            parentCombo.getItems().add(none);
            parentCombo.getItems().addAll(AppData.get().getGroupsIAmAdminOf());
            parentCombo.setValue(none);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setGroup(Group g) {
        this.existing = g;
        titleLabel.setText("Edit Group");
        nameField.setText(g.getName());
        descArea.setText(g.getDescription() != null ? g.getDescription() : "");
        Image img = Imgs.fromBytes(g.getProfilePicture());
        if (img != null) previewImg.setImage(img);
    }

    public void setOnClose(Runnable r) { this.onClose = r; }

    @FXML private void pickImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg"));
        File f = fc.showOpenDialog(saveBtn.getScene().getWindow());
        if (f != null) {
            try { picBytes = Imgs.toBytes(f); previewImg.setImage(new Image(f.toURI().toString())); }
            catch (Exception e) { err("Could not load image."); }
        }
    }

    @FXML private void doSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { err("Name is required."); return; }
        Integer parentId = null;
        Group sel = parentCombo.getValue();
        if (sel != null && sel.getId() != 0) parentId = sel.getId();
        try {
            if (existing != null) {
                Group group = new Group();
                group.setName(name);
                dao.update(existing.getId(), name, descArea.getText().trim(), picBytes);
                AppData.get().addOrUpdateGroup(group);
            } else {
                Group group=dao.create(name, descArea.getText().trim(), Session.uid(), parentId);
                AppData.get().addOrUpdateGroup(group);
            }
            //Navigator.closeModal(onClose);
            Navigator.close(titleLabel,onClose);
        } catch (Exception e) { err("Error: " + e.getMessage()); e.printStackTrace(); }
    }

    @FXML private void doCancel() {
        //Navigator.closeModal(onClose);
        Navigator.close(titleLabel,onClose);
    }
    private void err(String m) { errorLabel.setText(m); errorLabel.setVisible(true); }

}
