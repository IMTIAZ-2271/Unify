package com.Unify.controller;

import java.time.format.DateTimeFormatter;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.CanteenDAO;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Canteen;
import com.Unify.model.FoodItem;
import com.Unify.model.FoodOrder;
import com.Unify.model.Group;
import com.Unify.model.Notification;
import com.Unify.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CanteenController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private HBox canteenButtonBar;
    @FXML private TextField searchField;
    @FXML private Button cartButton;
    @FXML private Button addFoodBtn;
    @FXML private TableView<FoodItem> foodsTable;
    @FXML private TableColumn<FoodItem, String> colName;
    @FXML private TableColumn<FoodItem, Double> colPrice;
    @FXML private TableColumn<FoodItem, String> colCanteen;
    @FXML private TableColumn<FoodItem, Integer> colAvail;
    @FXML private TableColumn<FoodItem, Void> colAction;
    @FXML private Label statusLabel;

    private final GroupDAO groupDAO = new GroupDAO();
    private final CanteenDAO canteenDAO = new CanteenDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    private Group currentGroup;
    private int currentCanteenId = 0;
    private String currentCanteenName = "All";
    private boolean canManageFoods = false;
    private boolean canBuy = false;

    private final ObservableList<FoodItem> masterData = FXCollections.observableArrayList();
    private final ObservableList<FoodItem> cart = FXCollections.observableArrayList();
    private FilteredList<FoodItem> filteredData;

    @FXML
    public void initialize() {
        new Thread(() -> {
            try {
                canteenDAO.cancelOldOrders();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        setupTable();
        loadGroupsIntoDropdown();

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentGroup = newVal;
                loadCanteensForGroup(currentGroup);
            }
        });
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCanteen.setCellValueFactory(new PropertyValueFactory<>("canteenName"));
        colAvail.setCellValueFactory(new PropertyValueFactory<>("availableQty"));
        foodsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button buyBtn = new Button("Order");
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox manageBox = new HBox(5, editBtn, delBtn);

            {
                // Minimalist Table Buttons
                buyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #10B981; -fx-font-weight: bold; -fx-cursor: hand;");

                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));

                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));

                buyBtn.setOnAction(e -> {
                    FoodItem food = getTableView().getItems().get(getIndex());
                    if (food.getAvailableQty() > 0) {
                        if (cart.contains(food)) {
                            food.setCartQuantity(food.getCartQuantity() + 1);
                        } else {
                            food.setCartQuantity(1);
                            cart.add(food);
                        }
                        food.setAvailableQty(food.getAvailableQty() - 1);
                        foodsTable.refresh();
                        int totalCartItems = cart.stream().mapToInt(FoodItem::getCartQuantity).sum();
                        cartButton.setText("🛒 Cart (" + totalCartItems + ")");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Sold Out", "This item is currently out of stock.");
                    }
                });

                editBtn.setOnAction(e -> handleEditFood(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> handleDeleteFood(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (canBuy) {
                    setGraphic(buyBtn);
                } else if (canManageFoods) {
                    setGraphic(manageBox);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void loadGroupsIntoDropdown() {
        try {
            int userId = Session.currentUser().getId();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCanteensForGroup(Group group) {
        canteenButtonBar.getChildren().clear();
        try {
            int userId = Session.currentUser().getId();
            boolean isAdmin = groupDAO.isAdmin(group.getId(), userId);

            Button allBtn = new Button("All Canteens");
            allBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1E293B; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 5;");
            allBtn.setOnAction(e -> loadFoods(0, "All Canteens"));
            canteenButtonBar.getChildren().add(allBtn);

            List<Canteen> canteens = canteenDAO.getCanteensByGroup(group.getId());
            for (Canteen c : canteens) {
                Button btn = new Button(c.getName());
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1E293B; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 5;");
                btn.setOnAction(e -> loadFoods(c.getId(), c.getName()));
                canteenButtonBar.getChildren().add(btn);
            }

            if (isAdmin) {
                Button addBtn = new Button("+ Add Canteen");
                addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-border-color: #3B82F6; -fx-border-width: 1; -fx-border-radius: 5;");
                addBtn.setOnAction(e -> handleAddCanteen());
                canteenButtonBar.getChildren().add(addBtn);
            }

            loadFoods(0, "All Canteens");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFoods(int canteenId, String canteenName) {
        this.currentCanteenId = canteenId;
        this.currentCanteenName = canteenName;

        try {
            int userId = Session.currentUser().getId();
            boolean isAdmin = groupDAO.isAdmin(currentGroup.getId(), userId);
            boolean isManagerForThisCanteen = false;
            boolean isModeratorAnywhere = false;

            for (User u : groupDAO.members(currentGroup.getId())) {
                if (u.getId() == userId && u.getBio() != null) {
                    if (u.getBio().startsWith("moderator")) {
                        isModeratorAnywhere = true;
                    }
                    if (u.getBio().startsWith("moderator:canteen_manager:" + canteenName)) {
                        isManagerForThisCanteen = true;
                    }
                }
            }

            this.canManageFoods = isAdmin || isManagerForThisCanteen;
            if (canteenId == 0 && !isAdmin) {
                this.canManageFoods = false;
            }

            this.canBuy = !isAdmin && !isModeratorAnywhere;

            addFoodBtn.setVisible(this.canManageFoods);
            addFoodBtn.setManaged(this.canManageFoods);

            cartButton.setVisible(this.canBuy);
            cartButton.setManaged(this.canBuy);

            colAction.setVisible(this.canBuy || this.canManageFoods);

            List<FoodItem> foods = canteenDAO.getFoods(currentGroup.getId(), canteenId);
            masterData.setAll(foods);

            filteredData = new FilteredList<>(masterData, p -> true);
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(food -> {
                    if (newVal == null || newVal.isEmpty()) {
                        return true;
                    }
                    return food.getName().toLowerCase().contains(newVal.toLowerCase());
                });
            });

            SortedList<FoodItem> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(foodsTable.comparatorProperty());
            foodsTable.setItems(sortedData);

            statusLabel.setText("Viewing: " + canteenName + " (" + foods.size() + " items)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddCanteen() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Canteen");
        dialog.setHeaderText("Add a new canteen to " + currentGroup.getName());
        dialog.setContentText("Canteen Name (E.g., BUET Central Canteen):");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    canteenDAO.addCanteen(currentGroup.getId(), name.trim());
                    loadCanteensForGroup(currentGroup);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void handleAddFood() {
        if (currentCanteenId == 0) {
            showAlert(Alert.AlertType.WARNING, "Select a Canteen", "Please select a specific canteen from the top bar before adding food.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Food");
        dialog.setHeaderText("Add to " + currentCanteenName);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("E.g., Chicken Burger");
        TextField priceField = new TextField();
        priceField.setPromptText("Price");

        Spinner<Integer> qtySpinner = new Spinner<>(1, 1000, 10);
        qtySpinner.setEditable(true);

        grid.add(new Label("Food Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(qtySpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait().ifPresent(res -> {
            if (res == saveBtn) {
                try {
                    double price = Double.parseDouble(priceField.getText().trim());
                    canteenDAO.addFood(currentCanteenId, nameField.getText().trim(), price, qtySpinner.getValue());
                    loadFoods(currentCanteenId, currentCanteenName);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid price or database error.");
                }
            }
        });
    }

    private void handleEditFood(FoodItem food) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Food");
        dialog.setHeaderText("Update " + food.getName());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(food.getName());
        TextField priceField = new TextField(String.valueOf(food.getPrice()));
        Spinner<Integer> qtySpinner = new Spinner<>(0, 1000, food.getAvailableQty());
        qtySpinner.setEditable(true);

        grid.add(new Label("Food Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(qtySpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait().ifPresent(res -> {
            if (res == saveBtn) {
                try {
                    double price = Double.parseDouble(priceField.getText().trim());
                    canteenDAO.updateFood(food.getId(), nameField.getText().trim(), price, qtySpinner.getValue());
                    loadFoods(currentCanteenId, currentCanteenName);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid price or database error.");
                }
            }
        });
    }

    private void handleDeleteFood(FoodItem food) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + food.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    canteenDAO.deleteFood(food.getId());
                    loadFoods(currentCanteenId, currentCanteenName);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not delete the item.");
                }
            }
        });
    }

    @FXML
    public void showCart() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Cart Empty", "Add some food first!");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Your Cart");
        ButtonType orderBtn = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(orderBtn, ButtonType.CANCEL);

        ListView<FoodItem> listView = new ListView<>();
        listView.setItems(cart);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(FoodItem f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(f.getName() + " x" + f.getCartQuantity() + " - " + f.getCanteenName() + " (" + (f.getPrice() * f.getCartQuantity()) + ")");
                    Button rm = new Button("❌");
                    rm.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    rm.setOnAction(e -> {
                        f.setCartQuantity(f.getCartQuantity() - 1);
                        f.setAvailableQty(f.getAvailableQty() + 1);
                        if (f.getCartQuantity() <= 0) {
                            cart.remove(f);
                        }
                        listView.refresh();
                        foodsTable.refresh();
                        int totalCartItems = cart.stream().mapToInt(FoodItem::getCartQuantity).sum();
                        cartButton.setText("🛒 Cart (" + totalCartItems + ")");
                    });
                    setGraphic(rm);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });
        dialog.getDialogPane().setContent(listView);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == orderBtn) {
            int userId = Session.currentUser().getId();
            String orderBatchId = UUID.randomUUID().toString();
            for (FoodItem item : cart) {
                for (int i = 0; i < item.getCartQuantity(); i++) {
                    try {
                        boolean success = canteenDAO.placeOrder(userId, item.getId(), orderBatchId);
                        if (success) {
                            notifDAO.create(new Notification(
                                    userId,
                                    "Order Placed",
                                    "Your order for " + item.getName() + " has been sent to " + item.getCanteenName() + ".",
                                    "order_placed"
                            ));
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Out of Stock", item.getName() + " was sold out while you were browsing.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            cart.clear();
            cartButton.setText("🛒 Cart (0)");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Your food has been ordered! Check your notifications.");
            loadFoods(currentCanteenId, currentCanteenName);
        }
    }

    @FXML
    public void goBack() {
        Navigator.goTo("/com/Unify/fxml/utility.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void showOrders() {
        if (currentGroup == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(canManageFoods ? "Manage Incoming Orders" : "My Orders");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(750, 600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");

        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));
        scrollPane.setContent(contentBox);

        Runnable[] loadOrderDataRef = new Runnable[1];
        loadOrderDataRef[0] = () -> {
            contentBox.getChildren().clear();
            try {
                List<FoodOrder> orders;

                if (canManageFoods) {
                    orders = canteenDAO.getActiveOrdersForManager(currentGroup.getId(), currentCanteenId);
                    if (orders.isEmpty()) {
                        contentBox.getChildren().add(new Label("No active orders right now."));
                        return;
                    }

                    Map<String, List<FoodOrder>> groupedByBatch = new LinkedHashMap<>();
                    for (FoodOrder order : orders) {
                        String batchKey = order.getOrderBatchId();
                        if (batchKey == null || batchKey.isBlank()) {
                            batchKey = "legacy-" + order.getId();
                        }
                        groupedByBatch.computeIfAbsent(batchKey, key -> new ArrayList<>()).add(order);
                    }

                    for (List<FoodOrder> orderBatch : groupedByBatch.values()) {
                        contentBox.getChildren().add(createFlatOrderCard(orderBatch, loadOrderDataRef[0]));
                    }
                } else {
                    orders = canteenDAO.getActiveOrdersForUser(Session.currentUser().getId(), currentGroup.getId());
                    if (orders.isEmpty()) {
                        contentBox.getChildren().add(new Label("You have no active orders."));
                        return;
                    }

                    Map<String, List<FoodOrder>> groupedByBatch = new LinkedHashMap<>();
                    for (FoodOrder order : orders) {
                        String batchKey = order.getOrderBatchId();
                        if (batchKey == null || batchKey.isBlank()) {
                            batchKey = "legacy-" + order.getId();
                        }
                        groupedByBatch.computeIfAbsent(batchKey, key -> new ArrayList<>()).add(order);
                    }

                    for (List<FoodOrder> orderBatch : groupedByBatch.values()) {
                        contentBox.getChildren().add(createFlatStudentOrderCard(orderBatch, loadOrderDataRef[0]));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        loadOrderDataRef[0].run();
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private VBox createFlatOrderCard(List<FoodOrder> orderBatch, Runnable refresh) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-width: 2; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        FoodOrder firstOrder = orderBatch.get(0);
        String userName = firstOrder.getUserName() != null ? firstOrder.getUserName() : "Unknown User";

        String timeString = "Unknown Time";
        if (firstOrder.getOrderTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
            timeString = firstOrder.getOrderTime().format(formatter);
        }

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("USER: " + userName);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1E293B;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLbl = new Label("DATE/TIME: " + timeString);
        timeLbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13;");

        header.getChildren().addAll(nameLbl, spacer, timeLbl);
        card.getChildren().addAll(header, new Separator());

        for (String line : summarizeOrderItems(orderBatch)) {
            Label itemLabel = new Label("  - " + line);
            itemLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14;");
            card.getChildren().add(itemLabel);
        }

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        boolean hasPending = orderBatch.stream().anyMatch(o -> "pending".equals(o.getStatus()));
        if (hasPending) {
            Button approveBtn = new Button("Approve This Order");
            approveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15 8 15; -fx-background-radius: 6;");
            approveBtn.setOnAction(e -> {
                try {
                    for (FoodOrder order : orderBatch) {
                        if ("pending".equals(order.getStatus())) {
                            canteenDAO.updateOrderStatus(order.getId(), "collected");
                        }
                    }
                    refresh.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            footer.getChildren().add(approveBtn);
        } else {
            Label readyLabel = new Label("Approved & Waiting for Student");
            readyLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-style: italic; -fx-font-weight: bold;");
            footer.getChildren().add(readyLabel);
        }

        card.getChildren().add(footer);
        return card;
    }

    private List<String> summarizeOrderItems(List<FoodOrder> orders) {
        Map<String, Long> counts = orders.stream().collect(Collectors.groupingBy(
                o -> o.getFoodName() + " (" + o.getCanteenName() + ")",
                LinkedHashMap::new,
                Collectors.counting()
        ));

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            lines.add(entry.getValue() + " x " + entry.getKey());
        }
        return lines;
    }

    private VBox createFlatStudentOrderCard(List<FoodOrder> orderBatch, Runnable refresh) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-width: 2; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        FoodOrder firstOrder = orderBatch.get(0);
        String canteenName = firstOrder.getCanteenName() != null ? firstOrder.getCanteenName() : "Unknown Canteen";

        String timeString = "Unknown Time";
        if (firstOrder.getOrderTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
            timeString = firstOrder.getOrderTime().format(formatter);
        }

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("CANTEEN: " + canteenName);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1E293B;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLbl = new Label("DATE/TIME: " + timeString);
        timeLbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13;");

        header.getChildren().addAll(nameLbl, spacer, timeLbl);
        card.getChildren().addAll(header, new Separator());

        for (String line : summarizeOrderItems(orderBatch)) {
            Label itemLabel = new Label("  - " + line);
            itemLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14;");
            card.getChildren().add(itemLabel);
        }

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        boolean isReady = orderBatch.stream().anyMatch(o -> "collected".equals(o.getStatus()));

        if (isReady) {
            Label statusLbl = new Label("Ready to Collect!");
            statusLbl.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");

            Region footSpacer = new Region();
            HBox.setHgrow(footSpacer, Priority.ALWAYS);

            Button receiveBtn = new Button("Mark Received");
            receiveBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15 8 15; -fx-background-radius: 6;");
            receiveBtn.setOnAction(e -> {
                try {
                    for (FoodOrder o : orderBatch) {
                        if ("collected".equals(o.getStatus())) {
                            canteenDAO.updateOrderStatus(o.getId(), "received");
                        }
                    }
                    refresh.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            footer.getChildren().addAll(statusLbl, footSpacer, receiveBtn);
        } else {
            Label waitingLbl = new Label("Status: Preparing...");
            waitingLbl.setStyle("-fx-text-fill: #F59E0B; -fx-font-style: italic; -fx-font-weight: bold;");
            footer.getChildren().add(waitingLbl);
        }

        card.getChildren().add(footer);
        return card;
    }
}