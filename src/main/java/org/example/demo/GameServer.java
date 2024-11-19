package org.example.demo;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Map<String, List<PlayerHandler>> waitingPlayersMap = new HashMap<>();
    Map<String, Game> activeGames = new HashMap<>();
    List<PlayerInfo> allPlayers = new ArrayList<>();
    int count;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started, waiting for players...");

        while (true) {
            Socket playerSocket = serverSocket.accept();
            PlayerHandler playerHandler = new PlayerHandler(playerSocket);
            synchronized (allPlayers) {
                allPlayers.add(new PlayerInfo(playerHandler.getName(), " ", "Waiting", 0, " "));
            }
            playerHandler.start();
            List<PlayerHandler> L = new ArrayList<>();
            L.add(playerHandler);
            broadcastPlayerStatus(L);
        }
    }

    private synchronized void matchPlayers(PlayerHandler player, String boardSize) {
        List<PlayerHandler> waitingPlayers = waitingPlayersMap.computeIfAbsent(boardSize, k -> new ArrayList<>());

        if (waitingPlayers.isEmpty()) {
            waitingPlayers.add(player);
            updatePlayerStatus(player, boardSize, "Waiting", " ");
            player.sendMessage("Waiting for another player with board size " + boardSize + "...");
        } else {
            PlayerHandler opponent = waitingPlayers.remove(0);
            startGame(player, opponent, boardSize);
        }
        broadcastPlayerStatus(waitingPlayers);
    }

    private void updatePlayerStatus(PlayerHandler player, String board, String status, String id) {
        synchronized (allPlayers) {
            for (PlayerInfo info : allPlayers) {
                if (info.getName().equals(player.getName())) {
                    int rows = Integer.parseInt(board.split("x")[0]);
                    int columns = Integer.parseInt(board.split("x")[1]);
                    String s = (rows - 2)+"x"+ (columns - 2);
                    System.out.println(s);
                    info.setGameId(id);
                    info.setBoardsize(s);
                    info.setStatus(status);
                    break;
                }
            }
        }
    }


    private void startGame(PlayerHandler player1, PlayerHandler player2, String boardSize) {

        int rows = Integer.parseInt(boardSize.split("x")[0]);
        int columns = Integer.parseInt(boardSize.split("x")[1]);
        String gameBoard = generateGameBoard(rows, columns);
        String s = (rows - 2)+"x"+ (columns - 2);

        count++;
        String id1 = "00";
        String id2 = "0";
        String id = "";
        if(count<10){
            id = id1 + String.valueOf(count);
        }else {
            id = id2 + String.valueOf(count);
        }

        Game game = new Game(StringToBoard(gameBoard), id);

        updatePlayerStatus(player1, boardSize, "In Game", id);
        updatePlayerStatus(player2, boardSize,"In Game", id);

        player1.sendMessage("PLAYER_STATUS " + serializePlayerList(allPlayers));
        activeGames.put(id, game);

        player1.setOpponent(player2);
        player2.setOpponent(player1);

        player1.setGame(game);
        player2.setGame(game);

        game.addPlayer(player1);
        game.addPlayer(player2);

        player1.sendMessage(id+"START_GAME " + gameBoard);
        player2.sendMessage(id+"START_GAME " + gameBoard);

        System.out.println("Match found! Starting game for board size " + s + " "+ id);
        System.out.println(id + "START_GAME " + gameBoard);

    }
    private String generateGameBoard(int rows, int columns) {

        int[][] board = new int[rows][columns];
        Random ran = new Random();

        List<Integer> pairs = new ArrayList<>();
        int numPairs = (rows - 2) * (columns - 2) / 2;
        for (int i = 0; i < numPairs; i++) {
            int randomNum = ran.nextInt(1, 11);
            pairs.add(randomNum);
            pairs.add(randomNum);
        }
        Collections.shuffle(pairs);
        int index = 0;
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < columns - 1; c++) {
                board[r][c] = pairs.get(index);
                index++;
            }
        }
        String s = boardToString(board);

        return s;
    }

    private int[][] StringToBoard(String boardData) {
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

    private String boardToString(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                sb.append(board[r][c]);
                if (c < board[r].length - 1) {
                    sb.append(",");
                }
            }
            if (r < board.length - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public void updateGameBoard(String gameId, String boardData) {
        Game game = activeGames.get(gameId);
        if (game != null) {
            for (PlayerHandler p : game.getPlayers()) {
                p.sendMessage(gameId + "UPDATE_BOARD " + boardData);
            }
        }
    }

    private void broadcastPlayerStatus(List<PlayerHandler> l) {
        synchronized (allPlayers) {
            for (PlayerHandler player : l) {
                player.sendMessage("PLAYER_STATUS " + serializePlayerList(allPlayers));
            }
        }
    }
    public static String serializePlayerList(List<PlayerInfo> players) {
        StringBuilder sb = new StringBuilder();
        for (PlayerInfo player : players) {
            sb.append(player.getName()).append(",")
                    .append(player.getBoardSize()).append(",")
                    .append(player.getStatus()).append(",")
                    .append(player.getScore()).append(",")
                    .append(player.getGameId()).append(";");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        new GameServer().start(6666);
    }

    public class PlayerHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        public PlayerHandler opponent;
        private String boardSize;
        Game game;

        public PlayerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {

                    if (message.startsWith("BOARD_SIZE")) {
                        boardSize = message.split(" ")[1] + "x" + message.split(" ")[2];
                        matchPlayers(this, boardSize);
                    }
                    if(message.equals("TURN_DONE")){
                        opponent.sendMessage("TURN_DONE");
                        System.out.println(opponent);
                    }
                    if(message.startsWith("RESET")){
                        String chicun = message.substring(5).trim();
                        startGame(this, opponent, chicun);
                    }
//                    if (message.startsWith("PLAYER_STATUS")){
//                        this.sendMessage(message);
//                    }

                        String id = message.substring(0,3);
                    message = message.replace(id,"");
                    if (message.startsWith("UPDATE_BOARD")) {
//                        if (opponent != null) {
//                            opponent.sendMessage(message);
//                        }
                        message = message.replace("UPDATE_BOARD","");
                        updateGameBoard(id, message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void setOpponent(PlayerHandler opponent) {
            this.opponent = opponent;
        }
        public void setGame(Game game) {
            this.game = game;
        }
    }
}
