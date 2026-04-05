package com.Unify.controller;

import com.Unify.Session;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.TransportDAO;
import com.Unify.model.Bus;
import com.Unify.model.Group;
import com.Unify.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.Unify.Navigator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TransportController implements Initializable {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private TableView<Bus> busTable;
    @FXML private TableColumn<Bus, String> colBusNumber;
    @FXML private TableColumn<Bus, String> colRoute;
    @FXML private TableColumn<Bus, java.sql.Time> colTime;
    @FXML private TableColumn<Bus, String> colType;
    @FXML private TableColumn<Bus, String> colMessage;

    // NEW: Action column for inline Edit/Delete buttons
    @FXML private TableColumn<Bus, Void> colAction;

    @FXML private TextField searchField;
    @FXML private Button addBusButton;

    private TransportDAO transportDAO;
    private GroupDAO groupDAO;
    private ObservableList<Bus> masterBusList;
    private Group currentGroup;

    private boolean canManageTransport = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        transportDAO = new TransportDAO();
        groupDAO = new GroupDAO();
        masterBusList = FXCollections.observableArrayList();
        busTable.setItems(masterBusList);

        setupSearchFilter();
        setupActionColumn(); // Setup the minimalist buttons

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentGroup = newVal;
                loadScheduleData();
                checkPermissions();
            }
        });

        loadGroupsIntoDropdown();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox actionBox = new HBox(10, editBtn, delBtn);

            {
                // Minimalist Table Buttons
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));

                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));

                editBtn.setOnAction(e -> {
                    Bus selectedBus = getTableView().getItems().get(getIndex());
                    openBusForm(selectedBus);
                });

                delBtn.setOnAction(e -> {
                    Bus selectedBus = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selectedBus.getBusNumber() + "?");
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        if (transportDAO.deleteBus(selectedBus.getBusNumber())) {
                            masterBusList.remove(selectedBus);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (canManageTransport) {
                    setGraphic(actionBox); // Show only if they are an admin/ticket_manager
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void loadGroupsIntoDropdown() {
        try {
            int userId = Session.uid();
            List<Group> myGroups = groupDAO.myGroups(userId);
            groupComboBox.setItems(FXCollections.observableArrayList(myGroups));

            groupComboBox.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Group group, boolean empty) {
                    super.updateItem(group, empty);
                    setText(empty || group == null ? null : group.getName());
                }
            });
            groupComboBox.setButtonCell(groupComboBox.getCellFactory().call(null));

            if (!myGroups.isEmpty()) {
                groupComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadScheduleData() {
        if (currentGroup == null) return;
        List<Bus> busesFromDB = transportDAO.getAllBuses(currentGroup.getId());
        masterBusList.setAll(busesFromDB);
    }

    private void setupSearchFilter() {
        FilteredList<Bus> filteredData = new FilteredList<>(masterBusList, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(bus -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return bus.getRouteName().toLowerCase().contains(lowerCaseFilter) ||
                        bus.getBusNumber().toLowerCase().contains(lowerCaseFilter) ||
                        (bus.getMessage() != null && bus.getMessage().toLowerCase().contains(lowerCaseFilter));
            });
        });

        SortedList<Bus> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(busTable.comparatorProperty());
        busTable.setItems(sortedData);
    }

    private void checkPermissions() {
        if (currentGroup == null) return;

        canManageTransport = false;

        try {
            int uid = Session.uid();

            if (groupDAO.isAdmin(currentGroup.getId(), uid)) {
                canManageTransport = true;
            } else {
                for (User u : groupDAO.members(currentGroup.getId())) {
                    if (u.getId() == uid && u.getBio() != null && u.getBio().contains("ticket_manager")) {
                        canManageTransport = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Toggle UI elements based on permissions
        addBusButton.setVisible(canManageTransport);
        addBusButton.setManaged(canManageTransport);
        colAction.setVisible(canManageTransport); // Hide action column if student
        busTable.refresh(); // Refresh to render cells properly based on new permissions
    }

    @FXML
    public void goBack(){
        Navigator.goTo("/com/Unify/fxml/utility.fxml");
    }

    @FXML
    private void handleAddBus() {
        if (currentGroup != null) openBusForm(null);
    }

    private void openBusForm(Bus busToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/Unify/fxml/add_bus.fxml"));
            Parent root = loader.load();

            AddBusController controller = loader.getController();
            controller.setGroupId(currentGroup.getId());
            if (busToEdit != null) {
                controller.initData(busToEdit);
            }

            Stage stage = new Stage();
            stage.setTitle(busToEdit == null ? "Add New Bus" : "Edit Bus");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadScheduleData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}