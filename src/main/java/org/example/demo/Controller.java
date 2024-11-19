package org.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.Objects;

public class Controller {

    @FXML
    private Label scoreLabel;
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private Label boardOwnerLabel;
    @FXML
    public GridPane gameBoard;

    public Game game;
    public int point = 0;
    int[] position = new int[3];

    public ClientHandler clientHandler;

    boolean isYourTurn;

    public void setClientHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }
    public void setGame1(Game game1){
        this.game = game1;
    }

    @FXML
    public void initialize() {

    }
    public void updateScore(int increment) {
        point += increment;
        scoreLabel.setText(String.valueOf(point));
    }

    public void setPlayer() {
        boardOwnerLabel.setText(clientHandler.name);
    }
    public void setYourTurn(boolean isYourTurn) {
        this.isYourTurn = isYourTurn;
        if (isYourTurn) {
            currentPlayerLabel.setText("Your Turn");
        } else {
            currentPlayerLabel.setText("Opponent's Turn");
        }
    }
    public void createGameBoard(int[][] board, String id) {
        gameBoard.getChildren().clear();
        setPlayer();
        int r = board.length;
        int c = board[0].length;

        for (int row = 0; row < r; row++) {
            for (int col = 0; col < c; col++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                ImageView imageView = addContent(board[row][col]);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                button.setGraphic(imageView);
                int finalRow = row;
                int finalCol = col;
                button.setOnAction( event -> handleButtonPress(finalRow, finalCol));
                gameBoard.add(button, col, row);
            }
        }
        game = new Game(board, id);
        isYourTurn = true;
        clientHandler.setCurrentGameId(game.gameId);
    }
    public void UpdateBoard(int[][] board) {
        int r = board.length;
        int c = board[0].length;
        for (int row = 0; row < r; row++) {
            for (int col = 0; col < c; col++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                ImageView imageView = addContent(board[row][col]);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                button.setGraphic(imageView);
                int finalRow = row;
                int finalCol = col;
                button.setOnAction( event -> handleButtonPress(finalRow, finalCol));
                gameBoard.add(button, col, row);
            }
        }
    }

    private void handleButtonPress(int row, int col) {
        System.out.println(game.gameId+" Button pressed at: " + row + ", " + col);
        if(position[0] == 0){
            position[1] = row;
            position[2] = col;
            position[0] = 1;
        }else{
            boolean change = game.judge(position[1], position[2], row, col);
            position[0] = 0;
            if(change && isYourTurn){
                // TODO: handle the grid deletion logic
                game.removeBlock(position[1], position[2], row, col);
                this.updateScore(2);
                UpdateBoard(game.board);

                String boardData = serializeBoard(game.board);
                clientHandler.sendMessage(game.gameId+"UPDATE_BOARD " + boardData);
                isYourTurn = false;
                setYourTurn(false);

                clientHandler.sendMessage("TURN_DONE");
                System.out.println("Xiao Chu");
                //drawLine(position[1], position[2], row, col);
            }
            if(!change && isYourTurn){
                isYourTurn = false;
                setYourTurn(false);
                clientHandler.sendMessage("TURN_DONE");
            }
        }
    }
    private String serializeBoard(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) {
            for (int cell : row) {
                sb.append(cell).append(",");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    @FXML
    private void handleReset() {
        scoreLabel.setText("0");
        String s = game.row+"x"+game.col;
        clientHandler.sendMessage("RESET" + s);
    }

    public ImageView addContent(int content){
        return switch (content) {
            case 0 -> new ImageView(imageCarambola);
            case 1 -> new ImageView(imageApple);
            case 2 -> new ImageView(imageMango);
            case 3 -> new ImageView(imageBlueberry);
            case 4 -> new ImageView(imageCherry);
            case 5 -> new ImageView(imageGrape);
            case 6 -> new ImageView(imageKiwi);
            case 7 -> new ImageView(imageOrange);
            case 8 -> new ImageView(imagePeach);
            case 9 -> new ImageView(imagePear);
            case 10 -> new ImageView(imagePineapple);
            case 11 -> new ImageView(imageWatermelon);
            default -> null;
        };
    }

    public static Image imageApple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/apple.png")).toExternalForm());
    public static Image imageMango = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/mango.png")).toExternalForm());
    public static Image imageBlueberry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/blueberry.png")).toExternalForm());
    public static Image imageCherry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/cherry.png")).toExternalForm());
    public static Image imageGrape = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/grape.png")).toExternalForm());
    public static Image imageCarambola = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/carambola.png")).toExternalForm());
    public static Image imageKiwi = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/kiwi.png")).toExternalForm());
    public static Image imageOrange = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/orange.png")).toExternalForm());
    public static Image imagePeach = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/peach.png")).toExternalForm());
    public static Image imagePear = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pear.png")).toExternalForm());
    public static Image imagePineapple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pineapple.png")).toExternalForm());
    public static Image imageWatermelon = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/watermelon.png")).toExternalForm());

}
