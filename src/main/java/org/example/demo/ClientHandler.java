package org.example.demo;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {
    private BufferedReader in;
    private PrintWriter out;
    private Application app;
    private String currentGameId;
    String name;

    public ClientHandler(Application app, String serverAddress, int serverPort) throws IOException {
        this.app = app;
        Socket socket = new Socket(serverAddress, serverPort);
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
                    System.out.println(playerData);
                    List<PlayerInfo> players = deserializePlayerList(playerData);
                    Platform.runLater(() -> app.updatePlayers(players));

                }
                String gameId = message.substring(0, 3);
                message = message.substring(3).trim();
                if (message.startsWith("START_GAME")) {
                    String boardData = message.substring(10).trim();;
                    currentGameId = gameId;
                    Platform.runLater(() -> app.loadGameBoard(boardData, this, currentGameId));
                } else if (message.startsWith("UPDATE_BOARD")) {
                    System.out.println(name);
                    String boardData = message.substring(12).trim();;
                    if (gameId.equals(currentGameId)) {
                        Platform.runLater(() -> app.updateGameBoard(boardData, this));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

