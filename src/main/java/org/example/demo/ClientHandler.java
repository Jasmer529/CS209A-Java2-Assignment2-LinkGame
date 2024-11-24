package org.example.demo;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {
    private BufferedReader in;
    private PrintWriter out;
    private Application app;
    private String currentGameId;

    Socket socket;
    String name;



    public ClientHandler(Application app, String serverAddress, int serverPort) throws IOException {
        this.app = app;
        socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(this::listenForServerMessages).start();
    }

    public void sendBoardSize(int rows, int columns) {
        out.println("BOARD_SIZE " + rows + " " + columns);
    }
    public void listenForServerMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("TURN_DONE")) {
                    Platform.runLater(() -> app.updateTurn(this));
                }
                if (message.startsWith("PLAYER_STATUS")) {
                    String playerData = message.substring("PLAYER_STATUS ".length());
                    List<PlayerInfo> players = deserializePlayerList(playerData);
                    app.playerInfos = players;
                    Platform.runLater(() -> app.updatePlayers(players));
                }
                if (message.startsWith("END")) {
                    String playerData = message.substring("END ".length());
                    List<PlayerInfo> players = deserializePlayerList(playerData);
                    Platform.runLater(() -> app.moveOut(this));
                    app.playerInfos = players;
                }
                if(message.startsWith("CLOSE")){
                    Platform.runLater(() -> app.showOneL(this));
                }
                if (message.startsWith("RECONNECT")){
                    String[] r = message.split(" ");
                    String boardData = r[1];
                    int point = Integer.parseInt(r[2]);
                    Platform.runLater(() -> app.loadGameBoard(boardData, this, currentGameId, point));
                }

                String gameId = message.substring(0, 3);
                message = message.substring(3).trim();
                if (message.startsWith("START_GAME")) {
                    String boardData = message.substring(10).trim();;
                    currentGameId = gameId;
                    Platform.runLater(() -> app.loadGameBoard(boardData, this, currentGameId, 0));
                } else if (message.startsWith("UPDATE_BOARD")) {
                    String boardData = message.substring(12).trim();;
                    if (gameId.equals(currentGameId)) {
                        Platform.runLater(() -> app.updateGameBoard(boardData, this));
                    }

                } else if(message.startsWith("GAME_OVER")){
                    String result = message.substring(9).trim();
                    String[] r = result.split(" ");
                    String end = r[0];
                    int p1 = Integer.parseInt(r[1]);
                    int p2 = Integer.parseInt(r[2]);
                    WriteHistory(this, end, p1);
                    Platform.runLater(() -> app.showGameOverDialog(end, p1, p2, this));
                }
            }
        } catch (IOException e) {
            System.err.println("Connection to the server lost.");
            handleServerDisconnection();
            e.printStackTrace();
        }
    }

    public void WriteHistory(ClientHandler handler, String result, int p1){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("History.txt", true))) {
            writer.write(handler.name + " " + "Result:"+result+"  Point:"+p1);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerDisconnection() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Server Disconnected");
            alert.setHeaderText(null);
            alert.setContentText("Connection to the server has been lost.");
            alert.showAndWait();
        });
    }

    public static List<PlayerInfo> deserializePlayerList(String data) {
        List<PlayerInfo> players = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return players;
        }

        String[] playerStrings = data.split(";");
        for (String playerString : playerStrings) {
            String[] attributes = playerString.split(",");
            if (attributes.length == 5) {
                String name = attributes[0];
                String size1 = attributes[1];
                String status = attributes[2];
                int score = Integer.parseInt(attributes[3]);
                String gameId = attributes[4];
                players.add(new PlayerInfo(name, size1, status, score, gameId));
            }
        }
        return players;
    }
    public void sendMessage(String message) {
        out.println(message);
    }
    public void setCurrentGameId(String gameId) {
        this.currentGameId = gameId;
    }
}

