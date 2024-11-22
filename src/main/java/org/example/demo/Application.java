package org.example.demo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

import javafx.scene.control.cell.PropertyValueFactory;


public class Application extends javafx.application.Application {

    @FXML
    private Button startButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button rgButton;

    @FXML
    private Button historyButton;
    private Map<ClientHandler, Controller> clientControllerMap = new HashMap<>();
    List<ClientHandler> handlers = new ArrayList<>();

    @FXML
    private TableView<PlayerInfo> playersTable;
    @FXML
    private TableColumn<PlayerInfo, String> playerNameColumn;
    @FXML
    private TableColumn<PlayerInfo, String> playerSizeColumn;
    @FXML
    private TableColumn<PlayerInfo, String> playerStatusColumn;
    @FXML
    private TableColumn<PlayerInfo, Integer> playerScoreColumn;
    @FXML
    private TableColumn<PlayerInfo, String> gameIdColumn;
    @FXML
    private TableColumn<ClientHandler, Void> reconnectColumn;
    @FXML
    private TableColumn<ClientHandler, Void> LeaveColumn;
    Stage board;
    ObservableList<PlayerInfo> playerData = FXCollections.observableArrayList();
    List<PlayerInfo> playerInfos;

    @FXML
    public void initialize() {
        playerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        playerSizeColumn.setCellValueFactory(new PropertyValueFactory<>("boardSize"));
        playerStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        playerScoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        gameIdColumn.setCellValueFactory(new PropertyValueFactory<>("gameId"));
        playersTable.setItems(playerData);
        addButtonToReconnectColumn();
        addButtonToLeaveColumn();
        addChoosePlayer();
    }


    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Application.class.getResource("MainPage.fxml"));
        AnchorPane root = loader.load();
        primaryStage.setTitle("Main Page");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

    }

    @FXML
    private void handleStartButtonClick() {
        try {
            SetOriginUp(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateButtonClick() {
        System.out.println(playerInfos.size());
        updatePlayers(playerInfos);
    }

    @FXML
    private void handleHistoryButtonClick() {
        Stage historyStage = new Stage();
        historyStage.setTitle("History Viewer");
        TextArea historyTextArea = new TextArea();
        historyTextArea.setEditable(false);

        String filePath = "History.txt";
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            content.append("Error reading file: ").append(e.getMessage());
        }

        historyTextArea.setText(content.toString());

        VBox layout = new VBox(historyTextArea);
        Scene scene = new Scene(layout, 400, 300);
        historyStage.setScene(scene);

        historyStage.initModality(Modality.APPLICATION_MODAL);
        historyStage.show();
    }

    @FXML
    private void handlergButtonClick() {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Register");
        usernameDialog.setHeaderText("Enter your username:");
        usernameDialog.setContentText("Username:");
        Optional<String> usernameResult = usernameDialog.showAndWait();

        if (usernameResult.isPresent()) {
            String username = usernameResult.get();

            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Register");
            passwordDialog.setHeaderText("Enter your password:");
            passwordDialog.setContentText("Password:");
            Optional<String> passwordResult = passwordDialog.showAndWait();

            if (passwordResult.isPresent()) {
                String password = passwordResult.get();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt", true))) {
                    writer.write(username + ":" + password);
                    writer.newLine();
                    System.out.println("User registered successfully!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 显示 SetupPage 的方法
    public void SetOriginUp(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("SetupPage.fxml"));
        VBox root = fxmlLoader.load();
        SetUp setUpController = fxmlLoader.getController();
        setUpController.setApplication(this);

        Scene scene = new Scene(root);
        stage.setTitle("Choose Board Size");
        stage.setScene(scene);
        stage.show();
    }

    public void startGame(int rows, int columns, String name) throws IOException {
        ClientHandler client = new ClientHandler(this, "127.0.0.1", 6666);
        client.name = name;
        handlers.add(client);
        client.sendBoardSize(rows, columns);
    }

    public void loadGameBoard(String boardData, ClientHandler client, String id, int point) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("board.fxml"));
                VBox root = fxmlLoader.load();
                Controller controller = fxmlLoader.getController();
                controller.setClientHandler(client);

                clientControllerMap.put(client, controller);

                int[][] parsedBoard = parseBoardData(boardData);
                controller.createGameBoard(parsedBoard, id, point);

                Stage gameStage = new Stage();
                board = gameStage;
                gameStage.setTitle("Game Board");
                gameStage.setScene(new Scene(root));
                gameStage.show();

                controller.closeChu();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private int[][] parseBoardData(String boardData) {
        String[] rows = boardData.split(";");
        int[][] board = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            board[i] = new int[cells.length];
            for (int j = 0; j < cells.length; j++) {
                board[i][j] = Integer.parseInt(cells[j]);
            }
        }
        return board;
    }

    public void updateTurn(ClientHandler client){
        Platform.runLater(() -> {
            Controller controller = clientControllerMap.get(client);
            controller.isYourTurn = true;
            controller.setYourTurn(true);
        });
    }

    public void updateGameBoard(String boardData, ClientHandler client) {
        int[][] updatedBoard = parseBoardData(boardData);
        Platform.runLater(() -> {
            Controller controller = clientControllerMap.get(client);
            controller.game.board = updatedBoard;
            controller.UpdateBoard(updatedBoard);

        });
    }
    public void updatePlayers(List<PlayerInfo> players) {
        playerData.setAll(players);
        playersTable.setItems(playerData);
    }



    public void showGameOverDialog(String end, int p1, int p2, ClientHandler clientHandler) {
        Controller controller = clientControllerMap.get(clientHandler);
        controller.gameResultLabel.setText(end);
        controller.yourScoreLabel.setText(String.valueOf(p1));
        controller.opponentScoreLabel.setText(String.valueOf(p2));
        controller.scoreBox.setVisible(true);
    }

    public void showOneL(ClientHandler clientHandler) {
        Controller controller = clientControllerMap.get(clientHandler);
        controller.showOneLeave();
    }

    public void addChoosePlayer(){
        playersTable.setRowFactory(tv -> {
            TableRow<PlayerInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    PlayerInfo selectedPlayer = row.getItem();
                    System.out.println(selectedPlayer);
                    handlePlayerSelection(selectedPlayer);
                }
            });
            return row;
        });

    }
    private void handlePlayerSelection(PlayerInfo selectedPlayer) {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Login");
        usernameDialog.setHeaderText("Enter your username to start a game");
        usernameDialog.setContentText("Username:");

        Optional<String> usernameResult = usernameDialog.showAndWait();
        if (usernameResult.isPresent()) {
            String username = usernameResult.get();

            // 弹出密码输入框
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Login");
            passwordDialog.setHeaderText("Enter your password:");
            passwordDialog.setContentText("Password:");

            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (passwordResult.isPresent()) {
                String password = passwordResult.get();

                // 验证用户名和密码
                if (validateLogin(username, password)) {
                    System.out.println("Login successful! Starting game...");
                    String[] chicun = selectedPlayer.getBoardSize().split("x");
                    int row = Integer.parseInt(chicun[0]);
                    int col = Integer.parseInt(chicun[1]);
                    try {
                        startGame(row + 2, col + 2, username);
                    } catch (IOException e) {
                        System.err.println("Error starting the game: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Invalid username or password.");
                    showAlert("The username or password is incorrect. Please try again.");
                }
            }
        }
    }


    private void addButtonToReconnectColumn() {
        reconnectColumn.setCellFactory(column -> new TableCell<>() {
            private final Button reconnectButton = new Button("Reconnect");
            {
                reconnectButton.setOnAction(event -> {
                    ClientHandler player = handlers.get(getIndex());
                    System.out.println("Reconnecting player: " + player.name);
                    player.sendMessage("RECONNECT");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(reconnectButton);
                }
            }
        });
    }

    private void addButtonToLeaveColumn() {
        LeaveColumn.setCellFactory(column -> new TableCell<>() {
            private final Button leaveButton = new Button("Leave");
            {
                leaveButton.setOnAction(event -> {
                    ClientHandler player = handlers.get(getIndex());
                    System.out.println(player.name+" has left");
                    player.sendMessage("LEAVE");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(leaveButton);
                }
            }
        });
    }

    private boolean validateLogin(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
