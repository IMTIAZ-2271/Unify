package com.example.unify.calendar.controller;

import com.example.unify.calendar.view.MonthlyEventView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class calendarController {


    @FXML private Label monthLabel;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private GridPane calendarGrid;

    private MonthlyEventView monthlyEventView;


    @FXML
    public void initialize() {

        monthlyEventView = new MonthlyEventView(monthLabel, prevMonthBtn, nextMonthBtn, calendarGrid);
        monthlyEventView.initialize();
    }
}