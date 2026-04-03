package com.Unify.controller;

import com.Unify.Session;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.TransportDAO;
import com.Unify.dao.UserDAO;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TransportController implements Initializable {

    @FXML private ComboBox<Group> groupComboBox; // NEW
    @FXML private TableView<Bus> busTable;
    @FXML private TableColumn<Bus, String> colBusNumber;
    @FXML private TableColumn<Bus, String> colRoute;
    @FXML private TableColumn<Bus, java.sql.Time> colTime;
    @FXML private TableColumn<Bus, String> colType;
    @FXML private TableColumn<Bus, String> colMessage;

    @FXML private TextField searchField;
    @FXML private Button addBusButton;

    private TransportDAO transportDAO;
    private GroupDAO groupDAO; // NEW
    private ObservableList<Bus> masterBusList;
    private Group currentGroup; // NEW

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        transportDAO = new TransportDAO();
        groupDAO = new GroupDAO();
        masterBusList = FXCollections.observableArrayList();
        busTable.setItems(masterBusList);

        setupSearchFilter();

        // 1. ADD LISTENER FIRST: So it catches the auto-selection!
        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentGroup = newVal;
                loadScheduleData();
                checkPermissions();
            }
        });

        // 2. LOAD DATA SECOND: This triggers the listener we just added above
        loadGroupsIntoDropdown();
    }

    private void loadGroupsIntoDropdown() {
        try {
            int userId = Session.uid();
            List<Group> myGroups = groupDAO.myGroups(userId);
            groupComboBox.setItems(FXCollections.observableArrayList(myGroups));

            // Format the dropdown to show group names
            groupComboBox.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Group group, boolean empty) {
                    super.updateItem(group, empty);
                    setText(empty || group == null ? null : group.getName());
                }
            });
            groupComboBox.setButtonCell(groupComboBox.getCellFactory().call(null));

            // Auto-select the first group if available
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

        boolean canManageTransport = false;

        try {
            int uid = Session.uid();

            // 1. Automatically grant access if they are the Admin of the group
            if (groupDAO.isAdmin(currentGroup.getId(), uid)) {
                canManageTransport = true;
            } else {
                // 2. Otherwise, check if they were specifically assigned "ticket_manager"
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

        if (canManageTransport) {
            addBusButton.setVisible(true);
            addBusButton.setManaged(true);

            ContextMenu adminMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("✏️ Edit Selected Bus");
            editItem.setOnAction(event -> {
                Bus selectedBus = busTable.getSelectionModel().getSelectedItem();
                if (selectedBus != null) openBusForm(selectedBus);
            });

            MenuItem deleteItem = new MenuItem("🗑️ Delete Selected Bus");
            deleteItem.setOnAction(event -> {
                Bus selectedBus = busTable.getSelectionModel().getSelectedItem();
                if (selectedBus != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selectedBus.getBusNumber() + "?");
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        if (transportDAO.deleteBus(selectedBus.getBusNumber())) {
                            masterBusList.remove(selectedBus);
                        }
                    }
                }
            });

            adminMenu.getItems().addAll(editItem, deleteItem);
            busTable.setContextMenu(adminMenu);
        } else {
            addBusButton.setVisible(false);
            addBusButton.setManaged(false);
            busTable.setContextMenu(null);
        }
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
            // Pass the current Group ID so the AddBusController knows where to save it!
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