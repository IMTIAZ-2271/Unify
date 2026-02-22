package com.calendarapp.util;

/**
 * Auto-assigns deterministic, visually distinct colors to groups
 * based on group ID. Personal events always get PERSONAL_COLOR unless
 * the user picked a custom color.
 */
public class ColorUtil {

    public static final String PERSONAL_COLOR = "#6366F1"; // indigo

    /** Palette of 10 distinct, accessible colors for groups */
    private static final String[] PALETTE = {
        "#10B981", // emerald
        "#F59E0B", // amber
        "#EF4444", // red
        "#8B5CF6", // violet
        "#06B6D4", // cyan
        "#F97316", // orange
        "#EC4899", // pink
        "#14B8A6", // teal
        "#84CC16", // lime
        "#A855F7", // purple
    };

    /** Get the auto-assigned color for a group. */
    public static String forGroup(int groupId) {
        return PALETTE[Math.abs(groupId) % PALETTE.length];
    }

    /** Default event color (used when no group / personal) */
    public static String defaultPersonal() {
        return PERSONAL_COLOR;
    }
}
