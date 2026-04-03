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


public class LibraryController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private Label statusLabel;

    // Admin Controls
    @FXML private HBox adminControls;
    @FXML private HBox studentControls;

    // NEW UI Elements
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

    // Data handling for Search and Cart
    private ObservableList<Book> masterData = FXCollections.observableArrayList();
    private FilteredList<Book> filteredData;
    private ObservableList<Book> cart = FXCollections.observableArrayList();
    private Timeline autoRefresher;


    @FXML
    public void initialize() {
        loadGroupsIntoDropdown();


        //Hide the useless extra column by forcing columns to stretch perfectly
        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentGroup = newVal;
                loadLibraryForGroup(currentGroup);
            }
        });

        //  Decrease count locally when added to cart
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("➕");
            {
                btn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    if (book.getAvailableCopies() > 0 && !cart.contains(book)) {
                        cart.add(book);

                        // Decrease locally and refresh the table visually
                        book.setAvailableCopies(book.getAvailableCopies() - 1);
                        booksTable.refresh();

                        cartButton.setText("🛒 Cart (" + cart.size() + ")");
                    } else if (cart.contains(book)) {
                        showAlert(Alert.AlertType.WARNING, "Already in Cart", "You already added this book to your cart.");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Out of Stock", "Sorry, no available copies right now.");
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        // --- Real-Time Auto Refresher ---
        autoRefresher = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (currentGroup != null && (searchField.getText() == null || searchField.getText().isEmpty())) {
                try {
                    List<Book> freshBooks = libraryDAO.getBooksByGroup(currentGroup.getId());

                    // NEW: Prevent the refresher from overwriting items sitting in your cart!
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

            // Check if the current user is a librarian moderator
            for (User u : groupDAO.members(group.getId())) {
                if (u.getId() == userId && u.getBio() != null && u.getBio().startsWith("moderator:librarian")) {
                    isLibrarian = true;
                    break;
                }
            }

            // Grant access if they are an admin OR a librarian
            boolean hasManagerAccess = isAdmin || isLibrarian;

            adminControls.setVisible(hasManagerAccess);
            adminControls.setManaged(hasManagerAccess);

            // Hide Cart and '+' from Admins and Librarians
            cartButton.setVisible(!hasManagerAccess);
            cartButton.setManaged(!hasManagerAccess);
            colAction.setVisible(!hasManagerAccess);

            List<Book> books = libraryDAO.getBooksByGroup(group.getId());
            masterData.setAll(books);

            // Setup Live Search
            filteredData = new FilteredList<>(masterData, p -> true);
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(book -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return book.getTitle().toLowerCase().contains(lower) ||
                            (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lower));
                });
            });

            // Native Alphabetical Sorting
            SortedList<Book> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(booksTable.comparatorProperty());
            booksTable.setItems(sortedData);

            cart.clear();
            cartButton.setText("🛒 Cart (0)");
            statusLabel.setText("Showing books for: " + group.getName() + " (" + books.size() + " total)");

        } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML
    public void handleAddBook() {
        if (currentGroup == null) return;

        // 1. Create a custom dialog window
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Book");
        dialog.setHeaderText("Enter details for the new book in: " + currentGroup.getName());

        // 2. Add Save and Cancel buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 3. Build the form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField titleField = new TextField();
        titleField.setPromptText("E.g., Intro to Algorithms");

        TextField authorField = new TextField();
        authorField.setPromptText("E.g., Thomas H. Cormen");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Brief description...");
        descArea.setPrefRowCount(3);

        Spinner<Integer> copiesSpinner = new Spinner<>(1, 100, 1); // min 1, max 100, default 1

        // Add fields to the grid (Column, Row)
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Author:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);
        grid.add(new Label("Total Copies:"), 0, 3);
        grid.add(copiesSpinner, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Auto-focus the title field when it opens
        Platform.runLater(titleField::requestFocus);

        // 4. Show dialog and wait for the user to click Save
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {

            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String desc = descArea.getText().trim();
            int copies = copiesSpinner.getValue();

            // Simple validation
            if (title.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Book title cannot be empty.");
                return;
            }

            // 5. Save to database
            try {
                libraryDAO.addBook(currentGroup.getId(), title, author, desc, copies);
                loadLibraryForGroup(currentGroup); // Refresh the table
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

                        // Restore the count locally if they remove it from the cart
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
                        // Notify both admins and librarians (using startsWith to ignore the specific location tag)
                        if ("admin".equals(roleBio) || (roleBio != null && roleBio.startsWith("moderator:librarian"))) {
                            // Rich Notification String
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
    public void handleSettingsClick() {
        if (currentGroup == null) return;


    }

    @FXML
    public void goBack() {
        Navigator.goTo("/com/Unify/fxml/utilities.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}