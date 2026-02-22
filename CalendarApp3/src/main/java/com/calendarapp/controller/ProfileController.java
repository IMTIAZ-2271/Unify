package com.calendarapp.controller;

import com.calendarapp.Session;
import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.User;
import com.calendarapp.util.Crypto;
import com.calendarapp.util.Imgs;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class ProfileController {

    @FXML private ImageView avatarView;
    @FXML private Label usernameLabel, emailLabel, sinceLabel;
    @FXML private TextField displayField;
    @FXML private TextArea  bioArea;
    @FXML private PasswordField curPassField, newPassField, confirmPassField;
    @FXML private Label profileErr, passErr, successMsg;

    private byte[] newPicBytes;
    private final UserDAO dao = new UserDAO();
    private User user;

    @FXML private void initialize() {
        user = Session.currentUser();
        Image img = Imgs.fromBytes(user.getProfilePicture());
        if (img != null) avatarView.setImage(img);
        Imgs.circle(avatarView, 50);
        usernameLabel.setText("@" + user.getUsername());
        emailLabel.setText(user.getEmail());
        if (user.getCreatedAt() != null)
            sinceLabel.setText("Member since " + user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        displayField.setText(user.getDisplayName());
        bioArea.setText(user.getBio() != null ? user.getBio() : "");
        profileErr.setVisible(false);
        passErr.setVisible(false);
        successMsg.setVisible(false);
    }

    @FXML private void pickAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg"));
        File f = fc.showOpenDialog(avatarView.getScene().getWindow());
        if (f != null) {
            try {
                newPicBytes = Imgs.toBytes(f);
                avatarView.setImage(new Image(f.toURI().toString()));
            } catch (Exception e) { profileErr("Could not load image."); }
        }
    }

    @FXML private void saveProfile() {
        String dn = displayField.getText().trim();
        if (dn.isEmpty()) { profileErr("Display name cannot be empty."); return; }
        try {
            dao.updateProfile(user.getId(), dn, bioArea.getText().trim(), newPicBytes);
            user.setDisplayName(dn);
            user.setBio(bioArea.getText().trim());
            if (newPicBytes != null) user.setProfilePicture(newPicBytes);
            Session.login(user);
            profileErr.setVisible(false);
            success("Profile saved!");
        } catch (Exception e) { profileErr(e.getMessage()); }
    }

    @FXML private void changePassword() {
        String cur = curPassField.getText();
        String nw  = newPassField.getText();
        String cf  = confirmPassField.getText();
        if (cur.isEmpty() || nw.isEmpty() || cf.isEmpty()) { passErr("Fill all password fields."); return; }
        if (!Crypto.check(cur, user.getPasswordHash()))     { passErr("Current password is wrong."); return; }
        if (!nw.equals(cf))                                 { passErr("New passwords do not match."); return; }
        if (!Crypto.validPassword(nw))                      { passErr("Min 6 characters."); return; }
        try {
            dao.changePassword(user.getId(), nw);
            user.setPasswordHash(Crypto.hash(nw));
            curPassField.clear(); newPassField.clear(); confirmPassField.clear();
            passErr.setVisible(false);
            success("Password changed!");
        } catch (Exception e) { passErr(e.getMessage()); }
    }

    private void profileErr(String m) { profileErr.setText(m); profileErr.setVisible(true); }
    private void passErr(String m)    { passErr.setText(m); passErr.setVisible(true); }
    private void success(String m) {
        successMsg.setText(m); successMsg.setVisible(true);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> successMsg.setVisible(false));
        }).start();
    }
}
