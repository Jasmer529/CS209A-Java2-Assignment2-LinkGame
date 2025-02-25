package org.example.demo;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
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
    @FXML
    HBox scoreBox;
    @FXML
    Label yourScoreLabel;
    @FXML
    Label opponentScoreLabel;
    @FXML
    Label gameResultLabel;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private Label failureLabel;
    @FXML
    private Label leaveLabel;
    private GraphicsContext gc;

    public Game game;
    public int point = 0;
    int[] position = new int[3];

    public ClientHandler clientHandler;

    boolean isYourTurn;
    double cellWidth;
    double cellHeight;



    public void setClientHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @FXML
    public void initialize() {
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.widthProperty().bind(gameBoard.widthProperty());
        gameCanvas.heightProperty().bind(gameBoard.heightProperty());
        gameCanvas.setMouseTransparent(true);

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
    public void createGameBoard(int[][] board, String id, int p) {
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
                button.setOnAction(event -> handleButtonPress(finalRow, finalCol));
                gameBoard.add(button, col, row);
            }
        }
        game = new Game(board, id);
        isYourTurn = true;
        clientHandler.setCurrentGameId(game.gameId);
        scoreLabel.setText(String.valueOf(p));
        point = p;
        cellWidth = 45;
        cellHeight = 44;
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
            List<Point> pointList = game.judgePath(position[1], position[2], row, col);
            position[0] = 0;
            if(change && isYourTurn){
                // TODO: handle the grid deletion logic
                drawPath(pointList);
                game.removeBlock(position[1], position[2], row, col);
                this.updateScore(2);
                UpdateBoard(game.board);

                String boardData = serializeBoard(game.board);
                clientHandler.sendMessage(game.gameId+"UPDATE_BOARD" + boardData);
                isYourTurn = false;
                setYourTurn(false);

                if (game.checkEnd(game.board)) {
                    clientHandler.sendMessage(game.gameId+"GAME_OVER"+ point);
                    System.out.println("Game Over!");
                } else {
                    clientHandler.sendMessage("TURN_DONE");
                }

            }
            if(!change && isYourTurn){
                showFailureMessage();
                isYourTurn = false;
                setYourTurn(false);
                clientHandler.sendMessage("TURN_DONE");
            }
        }
    }

    public void closeChu(){
        Stage stage = (Stage) scoreLabel.getScene().getWindow();
        System.out.println(stage);
        stage.setOnCloseRequest(event -> {
            System.out.println("Window is closing...");
            clientHandler.sendMessage("CLOSE");


        });
    }


    private void drawPath(List<Point> path) {
        if (path == null || path.size() < 2) return;
        gc.setStroke(Color.GREEN);
        gc.setLineWidth(2);

        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);

            gc.strokeLine(
                    (p1.y + 1.02) * (cellWidth),
                    (p1.x + 1.02) * (cellHeight) - 20,
                    (p2.y + 1.02) * (cellWidth),
                    (p2.x + 1.02) * (cellHeight) - 20
            );
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(event -> clearCanvas());
        pause.play();
    }
    private void showFailureMessage() {
        failureLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> failureLabel.setVisible(false));
        pause.play();
    }

    public void showOneLeave() {
        leaveLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> leaveLabel.setVisible(false));
        pause.play();
    }


    private void clearCanvas() {
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
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
