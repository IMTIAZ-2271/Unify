package com.Unify.controller;

import com.Unify.App;
import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.dao.EventDAO;
import com.Unify.dao.GroupDAO;
import com.Unify.model.Event;
import com.Unify.model.Group;
import com.Unify.util.ColorUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MonthViewController {

    @FXML
    private Label monthYearLabel;
    @FXML
    private GridPane calGrid;
    @FXML
    private VBox filterPanel;   // left sidebar for group filters
    @FXML
    private VBox calendarArea;

    private YearMonth ym = YearMonth.now();

    // Filter state
    public static boolean showPersonal = true;
    public static final Map<Integer, Boolean> groupFilter = new LinkedHashMap<>(); // groupId → visible
    private final Map<Integer, Group> groupMap = new LinkedHashMap<>();

    private final EventDAO dao = new EventDAO();
    private final GroupDAO groupDAO = new GroupDAO();

    private DayViewController dayViewController;
    /*@FXML private void initialize() {
        loadGroupFilters();
        render();
    }*/


    @FXML
    private void initialize() {
        loadGroupFilters();
        render();
        // Re-render instantly whenever events change (e.g. sync from server)
        AppData.get().addEventsListener(this::render);
    }

    // Unregister when leaving
    public void onDestroy() {
        AppData.get().removeEventsListener(this::render);
    }


    // ── Filter panel ──────────────────────────────────────────────────────
    private void loadGroupFilters() {
        filterPanel.getChildren().clear();

        Label header = new Label("Show Events");
        header.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#475569;-fx-padding:0 0 6 0;");
        filterPanel.getChildren().add(header);

        // Personal checkbox
        CheckBox personalCb = new CheckBox("Personal");
        personalCb.setSelected(showPersonal);
        personalCb.setStyle("-fx-font-size:12px;");
        Circle dot0 = new Circle(6, Color.web(ColorUtil.defaultPersonal()));
        HBox pRow = new HBox(6, dot0, personalCb);
        pRow.setAlignment(Pos.CENTER_LEFT);
        pRow.setPadding(new Insets(2, 0, 2, 0));
        personalCb.selectedProperty().addListener((o, ov, nv) -> {
            showPersonal = nv;
            dayRender();
            render();
        });
        filterPanel.getChildren().add(pRow);

        // Group checkboxes
        try {
            List<Group> groups = AppData.get().getGroups();
            for (Group g : groups) {
                int id = g.getId();
                groupMap.put(id, g);
                groupFilter.putIfAbsent(id, true);

                String color = ColorUtil.forGroup(g.getId());
                Circle dot = new Circle(6, Color.web(color));

                CheckBox cb = new CheckBox(g.getName());
                cb.setSelected(groupFilter.get(id));
                cb.setStyle("-fx-font-size:12px;");
                cb.selectedProperty().addListener((o, ov, nv) -> {
                    groupFilter.put(id, nv);
                    dayRender();
                    render();
                });

                HBox row = new HBox(6, dot, cb);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(2, 0, 2, 0));
                filterPanel.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Calendar rendering ────────────────────────────────────────────────

    @FXML
    private void prevMonth() {
        ym = ym.minusMonths(1);
        render();
    }

    @FXML
    private void nextMonth() {
        ym = ym.plusMonths(1);
        render();
    }

    @FXML
    private void goToday() {
        ym = YearMonth.now();
        render();
    }

    @FXML
    private void newEvent() {
        openForm(null, LocalDate.now());
    }

    private void render() {
        monthYearLabel.setText(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calGrid.getChildren().clear();

        Map<LocalDate, List<Event>> byDay = new HashMap<>();
        try {
            List<Event> events = AppData.get().getEventsForMonth(
                    ym.getYear(), ym.getMonthValue());
            for (Event e : events) {
                if (e.isPersonal() && !showPersonal) continue;
                if (e.isGroup()) {
                    Integer gid = e.getGroupId();
                    if (gid != null && groupFilter.containsKey(gid) && !groupFilter.get(gid)) continue;
                }
                byDay.computeIfAbsent(e.getStartTime().toLocalDate(), k -> new ArrayList<>()).add(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        LocalDate today = LocalDate.now();
        int startCol = ym.atDay(1).getDayOfWeek().getValue() % 7;
        int days = ym.lengthOfMonth();
        int col = startCol, row = 0;

        for (int d = 1; d <= days; d++) {
            LocalDate date = ym.atDay(d);
            VBox cell = buildCell(date, byDay.getOrDefault(date, List.of()), today.equals(date));
            calGrid.add(cell, col, row);
            if (++col == 7) {
                col = 0;
                row++;
            }
        }
    }

    private VBox buildCell(LocalDate date, List<Event> events, boolean today) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4));
        cell.setMinHeight(95);
        cell.getStyleClass().addAll("day-cell", today ? "today-cell" : "");

        Label num = new Label(String.valueOf(date.getDayOfMonth()));
        num.getStyleClass().addAll("day-number", today ? "today-number" : "");
        cell.getChildren().add(num);

        int shown = 0;
        for (Event e : events) {
            if (shown == 3) {
                Label more = new Label("+" + (events.size() - 3) + " more");
                more.setStyle("-fx-text-fill:#64748B;-fx-font-size:10px;");
                cell.getChildren().add(more);
                break;
            }
            // Use auto-color for group events, stored color for personal
            String displayColor = e.isGroup() && e.getGroupId() != null
                    ? ColorUtil.forGroup(e.getGroupId())
                    : e.getColor();

            Label tag = new Label(e.getTitle());
            tag.setMaxWidth(Double.MAX_VALUE);
            tag.setPadding(new Insets(1, 5, 1, 5));
            tag.setStyle("-fx-background-color:" + displayColor
                    + ";-fx-text-fill:white;-fx-background-radius:3;-fx-font-size:11px;-fx-font-weight:bold;");
            tag.setCursor(javafx.scene.Cursor.HAND);
            Tooltip.install(tag, new Tooltip(e.getTitle()
                    + (e.getGroupName() != null ? " · " + e.getGroupName() : "")));

            final Event ev = e;
            tag.setOnMouseClicked(click -> {
                click.consume();
                openDetail(ev);
            });
            cell.getChildren().add(tag);
            shown++;
        }

        cell.setOnMouseClicked(click -> {
            if (click.getTarget() == cell || click.getTarget() instanceof Label && ((Label) click.getTarget()) == cell.getChildren().get(0)) {
                openDay(date);
            }
        });
        cell.setCursor(javafx.scene.Cursor.HAND);
        return cell;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private void openDay(LocalDate date) {
        try {
            FXMLLoader loader = App.loader("/com/Unify/fxml/day_view.fxml");
            Node node = loader.load();
            calendarArea.getChildren().setAll(node);
            dayViewController = loader.getController();
            dayViewController.setDate(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openDetail(Event e) {
        EventDetailController ctrl = Navigator.showWindow("/com/Unify/fxml/event_detail.fxml");
        if (ctrl != null) ctrl.setEvent(e, this::render);
    }

    private void openForm(Event existing, LocalDate def) {
        EventFormController ctrl = Navigator.showWindow("/com/Unify/fxml/event_form.fxml");
        if (ctrl != null) {
            if (existing != null) ctrl.setEvent(existing);
            else ctrl.setDefaultDate(def);
            ctrl.setOnClose(this::render);
        }
    }

    public void checkAllGroups() {
        groupFilter.replaceAll((key, oldValue) -> true);
    }

    public void uncheckAllGroups() {
        groupFilter.replaceAll((key, oldValue) -> false);
    }

    public void checkOrUncheckAll(ActionEvent event) {

    }

    public static void refreshCheckbox() {
        groupFilter.clear();
        showPersonal = true;
    }

    private void dayRender() {
        if (dayViewController != null) {
            dayViewController.setDate(dayViewController.getDate());
        }
    }
}
