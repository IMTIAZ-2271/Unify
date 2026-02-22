package com.calendarapp.util;

import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import javafx.scene.image.ImageView;

import java.io.*;
import java.nio.file.Files;

public class Imgs {

    public static byte[] toBytes(File f) throws IOException {
        return Files.readAllBytes(f.toPath());
    }

    public static Image fromBytes(byte[] b) {
        if (b == null || b.length == 0) return defaultAvatar();
        try { return new Image(new ByteArrayInputStream(b)); }
        catch (Exception e) { return defaultAvatar(); }
    }

    public static Image defaultAvatar() {
        // Inline SVG not possible; return null and handle in UI
        return null;
    }

    public static void circle(ImageView iv, double r) {
        Circle c = new Circle(r, r, r);
        iv.setClip(c);
    }

    public static void setAvatar(ImageView iv, byte[] pic, double size) {
        Image img = fromBytes(pic);
        if (img != null) iv.setImage(img);
        circle(iv, size / 2);
    }
}
