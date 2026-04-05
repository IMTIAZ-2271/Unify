package com.Unify.controller;

import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.LibraryDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Notification;

import com.Unify.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

public class LibraryController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private Label statusLabel;

    // NEW UI References to match flat FXML layout
    @FXML private Button requestsBtn;
    @FXML private Button addBookBtn;

    @FXML private TextField searchField;
    @FXML private Button cartButton;
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, String> colDesc;
    @FXML private TableColumn<Book, Integer> colAvail;
    @FXML private TableColumn<Book, Void> colAction;

    private final GroupDAO groupDAO = new GroupDAO();
    private final LibraryDAO libraryDAO = new LibraryDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private Group currentGroup;

    private ObservableList<Book> masterData = FXCollections.observableArrayList();
    private FilteredList<Book> filteredData;
    private ObservableList<Book> cart = FXCollections.observableArrayList();
    private Timeline autoRefresher;
    private boolean canManageLibrary = false;

    @FXML
    public void initialize() {
        loadGroupsIntoDropdown();

        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentGroup = newVal;
                loadLibraryForGroup(currentGroup);
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button addBtn = new Button("Borrow");
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox managerBox = new HBox(5, editBtn, delBtn);

            {
                // Minimalist Table Buttons
                addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #10B981; -fx-font-weight: bold; -fx-cursor: hand;");

                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));

                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand;");
                delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));


                addBtn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    if (book.getAvailableCopies() > 0 && !cart.contains(book)) {
                        cart.add(book);
                        book.setAvailableCopies(book.getAvailableCopies() - 1);
                        booksTable.refresh();
                        cartButton.setText("🛒 Cart (" + cart.size() + ")");
                    } else if (cart.contains(book)) {
                        showAlert(Alert.AlertType.WARNING, "Already in Cart", "You already added this book to your cart.");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Out of Stock", "Sorry, no available copies right now.");
                    }
                });

                editBtn.setOnAction(e -> handleEditBook(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> handleDeleteBook(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (canManageLibrary) {
                    setGraphic(managerBox);
                } else {
                    setGraphic(addBtn);
                }
            }
        });

        autoRefresher = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (currentGroup != null && (searchField.getText() == null || searchField.getText().isEmpty())) {
                try {
                    List<Book> freshBooks = libraryDAO.getBooksByGroup(currentGroup.getId());

                    for (Book freshBook : freshBooks) {
                        for (Book cartBook : cart) {
                            if (freshBook.getId() == cartBook.getId()) {
                                freshBook.setAvailableCopies(freshBook.getAvailableCopies() - 1);
                            }
                        }
                    }

                    masterData.setAll(freshBooks);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }));
        autoRefresher.setCycleCount(Animation.INDEFINITE);
        autoRefresher.play();
    }


    private void loadGroupsIntoDropdown() {
        try {
            int userId = Session.currentUser().getId();
            List<Group> myGroups = groupDAO.myGroups(userId);

            groupComboBox.setItems(FXCollections.observableArrayList(myGroups));
            groupComboBox.setCellFactory(param -> new ListCell<Group>() {
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

    private void loadLibraryForGroup(Group group) {
        try {
            int userId = Session.currentUser().getId();
            boolean isAdmin = groupDAO.isAdmin(group.getId(), userId);
            boolean isLibrarian = false;

            for (User u : groupDAO.members(group.getId())) {
                if (u.getId() == userId && u.getBio() != null && u.getBio().startsWith("moderator:librarian")) {
                    isLibrarian = true;
                    break;
                }
            }

            boolean hasManagerAccess = isAdmin || isLibrarian;

            requestsBtn.setVisible(hasManagerAccess);
            requestsBtn.setManaged(hasManagerAccess);
            addBookBtn.setVisible(hasManagerAccess);
            addBookBtn.setManaged(hasManagerAccess);

            cartButton.setVisible(!hasManagerAccess);
            cartButton.setManaged(!hasManagerAccess);
            canManageLibrary = hasManagerAccess;
            colAction.setVisible(true);

            List<Book> books = libraryDAO.getBooksByGroup(group.getId());
            masterData.setAll(books);

            filteredData = new FilteredList<>(masterData, p -> true);
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(book -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return book.getTitle().toLowerCase().contains(lower) ||
                            (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lower));
                });
            });

            SortedList<Book> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(booksTable.comparatorProperty());
            booksTable.setItems(sortedData);

            cart.clear();
            cartButton.setText("🛒 Cart (0)");
            statusLabel.setText("Showing books for: " + group.getName() + " (" + books.size() + " total)");

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleEditBook(Book book) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Book Inventory");
        dialog.setHeaderText("Update details for: " + book.getTitle());
        ButtonData saveButtonData = ButtonData.OK_DONE;
        ButtonType saveButtonType = new ButtonType("Save", saveButtonData);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField titleField = new TextField(book.getTitle());
        TextField authorField = new TextField(book.getAuthor());
        TextArea descArea = new TextArea(book.getDescription());
        descArea.setPrefRowCount(3);

        int initialTotal = book.getTotalCopies();
        int initialAvailable = book.getAvailableCopies();
        int initialLent = initialTotal - initialAvailable;

        if (initialLent < 0) {
            initialLent = 0;
            initialTotal = initialAvailable;
        }

        final int[] currentLent = {initialLent};

        Label totalLabel = new Label(String.valueOf(initialTotal));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lentLabel = new Label(String.valueOf(currentLent[0]));
        lentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #F59E0B;");

        Spinner<Integer> availableSpinner = new Spinner<>(0, 10000, initialAvailable);
        availableSpinner.setEditable(true);
        availableSpinner.setStyle("-fx-font-weight: bold;");

        Spinner<Integer> returnSpinner = new Spinner<>(0, Math.max(0, currentLent[0]), 0);
        returnSpinner.setEditable(true);

        availableSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            int newTotal = newVal + currentLent[0];
            totalLabel.setText(String.valueOf(newTotal));
        });

        Button applyReturnBtn = new Button("Apply Return");
        applyReturnBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        applyReturnBtn.setOnAction(e -> {
            int returnQty = returnSpinner.getValue();
            if (returnQty > 0 && returnQty <= currentLent[0]) {
                currentLent[0] -= returnQty;
                lentLabel.setText(String.valueOf(currentLent[0]));
                availableSpinner.getValueFactory().setValue(availableSpinner.getValue() + returnQty);
                ((SpinnerValueFactory.IntegerSpinnerValueFactory) returnSpinner.getValueFactory()).setMax(currentLent[0]);
                returnSpinner.getValueFactory().setValue(0);
            }
        });

        HBox returnBox = new HBox(10, returnSpinner, applyReturnBtn);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Author:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);
        grid.add(new Label("--- Inventory Sync ---"), 1, 3);
        grid.add(new Label("Total Copies:"), 0, 4);
        grid.add(totalLabel, 1, 4);
        grid.add(new Label("Lent Copies:"), 0, 5);
        grid.add(lentLabel, 1, 5);
        grid.add(new Label("Available Copies:"), 0, 6);
        grid.add(availableSpinner, 1, 6);
        grid.add(new Label("--- Actions ---"), 1, 7);
        grid.add(new Label("↩️ Return Books:"), 0, 8);
        grid.add(returnBox, 1, 8);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(titleField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                int finalAvailable = availableSpinner.getValue();
                int finalTotal = finalAvailable + currentLent[0];

                libraryDAO.updateBook(
                        book.getId(),
                        titleField.getText().trim(),
                        authorField.getText().trim(),
                        descArea.getText().trim(),
                        finalTotal,
                        finalAvailable
                );

                loadLibraryForGroup(currentGroup);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update the book.");
            }
        }
    }

    private void handleDeleteBook(Book book) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + book.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    libraryDAO.deleteBook(book.getId());
                    loadLibraryForGroup(currentGroup);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not delete the book. It might be linked to active requests.");
                }
            }
        });
    }

    @FXML
    public void handleAddBook() {
        if (currentGroup == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Book");
        dialog.setHeaderText("Enter details for the new book in: " + currentGroup.getName());
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("E.g., Intro to Algorithms");

        TextField authorField = new TextField();
        authorField.setPromptText("E.g., Thomas H. Cormen");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Brief description...");
        descArea.setPrefRowCount(3);

        Spinner<Integer> copiesSpinner = new Spinner<>(1, 100, 1);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Author:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);
        grid.add(new Label("Total Copies:"), 0, 3);
        grid.add(copiesSpinner, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(titleField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String desc = descArea.getText().trim();
            int copies = copiesSpinner.getValue();

            if (title.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Book title cannot be empty.");
                return;
            }

            try {
                libraryDAO.addBook(currentGroup.getId(), title, author, desc, copies);
                loadLibraryForGroup(currentGroup);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the book to the database.");
            }
        }
    }

    @FXML
    public void showCart() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Cart Empty", "Add books to your cart first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Your Cart");
        dialog.setHeaderText("Review your book requests");
        ButtonType checkoutBtn = new ButtonType("Checkout All", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, ButtonType.CANCEL);

        ListView<Book> listView = new ListView<>();
        listView.setItems(cart);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setText(null); setGraphic(null); }
                else {
                    setText("📖 " + b.getTitle() + " by " + b.getAuthor());
                    Button rm = new Button("❌");
                    rm.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    rm.setOnAction(e -> {
                        cart.remove(b);
                        b.setAvailableCopies(b.getAvailableCopies() + 1);
                        booksTable.refresh();
                        cartButton.setText("🛒 Cart (" + cart.size() + ")");
                    });
                    setGraphic(rm);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });
        dialog.getDialogPane().setContent(listView);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == checkoutBtn) processCheckout();
    }

    private void processCheckout() {
        int userId = Session.currentUser().getId();
        String reqTime = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        for (Book b : cart) {
            try {
                int issueId = libraryDAO.requestBook(b.getId(), userId);
                if (issueId != -1) {
                    List<User> members = groupDAO.members(currentGroup.getId());
                    for (User m : members) {
                        String roleBio = m.getBio();
                        if ("admin".equals(roleBio) || (roleBio != null && roleBio.startsWith("moderator:librarian"))) {
                            String msg = String.format("%s requested '%s' by %s from the %s library on %s.",
                                    Session.currentUser().getDisplayName(), b.getTitle(), b.getAuthor(), currentGroup.getName(), reqTime);

                            Notification n = new Notification(m.getId(), "📚 Book Request: " + b.getTitle(), msg, "book_request");
                            n.setReferenceId(issueId);
                            notifDAO.create(n);
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        showAlert(Alert.AlertType.INFORMATION, "Success", "Your books have been requested!");
        cart.clear();
        cartButton.setText("🛒 Cart (0)");
        loadLibraryForGroup(currentGroup);
    }

    @FXML
    public void showRequests() {
        if (currentGroup == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Book Requests");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(700, 500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");

        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));
        scrollPane.setContent(contentBox);

        Runnable[] loadRequestsRef = new Runnable[1];

        loadRequestsRef[0] = () -> {
            contentBox.getChildren().clear();
            try {
                List<BookIssue> requests = libraryDAO.getPendingRequests(currentGroup.getId());

                if (requests.isEmpty()) {
                    Label emptyLabel = new Label("No pending book requests right now.");
                    emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
                    contentBox.getChildren().add(emptyLabel);
                    return;
                }

                Map<Integer, List<BookIssue>> groupedRequests = new LinkedHashMap<>();
                for (BookIssue issue : requests) {
                    groupedRequests.computeIfAbsent(issue.getUserId(), k -> new ArrayList<>()).add(issue);
                }

                for (List<BookIssue> studentBatch : groupedRequests.values()) {
                    contentBox.getChildren().add(createStudentRequestCard(studentBatch, loadRequestsRef[0]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        loadRequestsRef[0].run();
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private VBox createStudentRequestCard(List<BookIssue> studentBatch, Runnable refresh) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-width: 2; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        BookIssue firstIssue = studentBatch.get(0);

        String timeString = "Unknown Time";
        if (firstIssue.getRequestedAt() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
            timeString = firstIssue.getRequestedAt().format(formatter);
        }

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("STUDENT: " + firstIssue.getRequesterName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1E293B;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label timeLbl = new Label("DATE/TIME: " + timeString);
        timeLbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13;");

        header.getChildren().addAll(nameLbl, headerSpacer, timeLbl);
        card.getChildren().addAll(header, new Separator());

        for (BookIssue issue : studentBatch) {
            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            Label bookLbl = new Label("  📖 " + issue.getBookTitle());
            bookLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 14; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button rejectBtn = new Button("Reject");
            rejectBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15 5 15;");
            rejectBtn.setOnAction(e -> {
                try {
                    libraryDAO.processRequestFromNotification(issue.getId(), false);
                    loadLibraryForGroup(currentGroup);
                    refresh.run();
                } catch(Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject request.");
                }
            });

            Button approveBtn = new Button("Approve");
            approveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15 5 15; -fx-background-radius: 6;");
            approveBtn.setOnAction(e -> {
                try {
                    libraryDAO.processRequestFromNotification(issue.getId(), true);
                    loadLibraryForGroup(currentGroup);
                    refresh.run();
                } catch(Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to approve request.");
                }
            });

            itemRow.getChildren().addAll(bookLbl, spacer, rejectBtn, approveBtn);
            card.getChildren().add(itemRow);
        }

        return card;
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
}