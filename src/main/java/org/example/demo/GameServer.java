package org.example.demo;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Map<String, List<PlayerHandler>> waitingPlayersMap = new HashMap<>();

    Map<PlayerHandler, Integer> pointMap = new HashMap<>();
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
                    info.setGameId(id);
                    info.setBoardsize(s);
                    info.setStatus(status);
                    break;
                }
            }
        }
    }

    private void updatePlayerStatus2(PlayerHandler player, String status, int score) {
        synchronized (allPlayers) {
            for (PlayerInfo info : allPlayers) {
                if (info.getName().equals(player.getName())) {
                    if(info.getCanChange()){
                        info.setScore(score);
                        info.setStatus(status);
                    }
                    if(status.equals("Game over")){
                        info.setCanChange(false);
                    }
                    break;
                }
            }
        }
    }

    private void startGame(PlayerHandler player1, PlayerHandler player2, String boardSize) {

        int rows = Integer.parseInt(boardSize.split("x")[0]);
        int columns = Integer.parseInt(boardSize.split("x")[1]);
        boolean temp = true;
        String gameBoard = "";
        if(rows < 6 || columns < 6){
            while(temp){
                gameBoard = generateGameBoard(rows, columns);
                temp = hasUnsolvableDiagonal(StringToBoard(gameBoard));
            }
        }else {
            gameBoard = generateGameBoard(rows, columns);
        }

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

        player2.sendMessage(id+"START_GAME " + gameBoard);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        player1.sendMessage(id+"START_GAME " + gameBoard);

        System.out.println("Match found! Starting game for board size " + s + " "+ id);
        System.out.println(id + "START_GAME " + gameBoard);

    }
    public boolean hasUnsolvableDiagonal(int[][] board) {
        int rows = board.length - 1;
        int cols = board[0].length - 1;

        for (int x = 1; x < rows - 1; x++) {
            for (int y = 1; y < cols - 1; y++) {
                int topLeft = board[x][y];
                int topRight = board[x][y + 1];
                int bottomLeft = board[x + 1][y];
                int bottomRight = board[x + 1][y + 1];
                if (topLeft == bottomRight && topRight == bottomLeft && topLeft != topRight) {
                    System.out.println("有坏东西");
                    return true;
                }
            }
        }

        return false;
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

    public void OverGame(String gameId){
        Game game = activeGames.get(gameId);
        PlayerHandler p1 = game.getPlayers().get(0);
        PlayerHandler p2 = game.getPlayers().get(1);
        int po1 = pointMap.get(p1);
        int po2 = pointMap.get(p2);
        WriteHistory2(game.row, game.col, gameId);

        if(po1 > po2){
            p1.sendMessage(gameId + "GAME_OVER"+"WIN "+po1+" "+po2);
            p2.sendMessage(gameId + "GAME_OVER"+"LOSE "+po2+" "+po1);
        }else if(po2 > po1){
            p2.sendMessage(gameId + "GAME_OVER"+"WIN "+po2+" "+po1);
            p1.sendMessage(gameId + "GAME_OVER"+"LOSE "+po1+" "+po2);
        }else {
            p2.sendMessage(gameId + "GAME_OVER"+"PING "+po2+" "+po1);
            p1.sendMessage(gameId + "GAME_OVER"+"PING "+po1+" "+po2);
        }
        updatePlayerStatus2(p1, "Game over", po1);
        updatePlayerStatus2(p2, "Game over", po2);
        p1.sendMessage("PLAYER_STATUS " + serializePlayerList(allPlayers));
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
    public void WriteHistory2(int row, int col, String id){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("History.txt", true))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = now.format(formatter);
            String chicun = (row - 2) + "x" + (col - 2);
            writer.write(formattedTime+" Game "+id+" of a "+chicun+" board");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        private String currentBoard;
        Game game;

        int score;
        boolean leave;

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
                    }
                    if(message.startsWith("RESET")){
                        String chicun = message.substring(5).trim();
                        startGame(this, opponent, chicun);
                    }
                    if(message.startsWith("CLOSE")){
                        this.leave = true;
                        opponent.sendMessage("CLOSE");
                        updatePlayerStatus2(this, "Leave", this.score);
                        if(this.leave && opponent.leave) {
                            System.out.println("Both players have left the game. Closing connections...");
                            synchronized (allPlayers) {
                                allPlayers.removeIf(playerInfo ->
                                        playerInfo.getName().equals(this.getName()) ||
                                                playerInfo.getName().equals(opponent.getName())
                                );
                            }
                            this.sendMessage("END "+ serializePlayerList(allPlayers));
                        }else {
                            this.sendMessage("PLAYER_STATUS " + serializePlayerList(allPlayers));
                        }


                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(this.leave && opponent.leave){
                            System.out.println("Both players have left the game. Closing connections...");
//                            synchronized (allPlayers) {
//                                allPlayers.removeIf(playerInfo ->
//                                        playerInfo.getName().equals(this.getName()) ||
//                                                playerInfo.getName().equals(opponent.getName())
//                                );
//                            }
                            closeConnections();
                            synchronized (activeGames) {
                                activeGames.remove(game.gameId);
                            }
                            logGameTermination(game.gameId, this.getName(), opponent.getName());
                            break;

                        }
                    }
                    if(message.startsWith("LEAVE")){
                        synchronized (allPlayers) {
                            allPlayers.removeIf(playerInfo ->
                                    playerInfo.getName().equals(this.getName())
                            );
                        }
                        List<PlayerHandler> waitingPlayers = waitingPlayersMap.get(boardSize);

                        if (waitingPlayers != null) {
                            waitingPlayers.remove(this);
                            if (waitingPlayers.isEmpty()) {
                                waitingPlayersMap.remove(boardSize);
                            }
                        }
                        this.sendMessage("END "+ serializePlayerList(allPlayers));
//                        try {
//                            Thread.sleep(2000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        closeConnections();
                    }

                    if(message.startsWith("RECONNECT")){
                        //
                        this.leave = false;
                        this.sendMessage("RECONNECT "+ this.currentBoard+" "+ this.score);
                        updatePlayerStatus2(this, "In Game", 0);
                        this.sendMessage("PLAYER_STATUS " + serializePlayerList(allPlayers));
                    }


                    String id = message.substring(0,3);
                    message = message.replace(id,"");
                    if (message.startsWith("UPDATE_BOARD")) {
//                        if (opponent != null) {
//                            opponent.sendMessage(message);
//                        }
                        message = message.replace("UPDATE_BOARD","");
                        updateGameBoard(id, message);
                        opponent.currentBoard = message;
                        this.currentBoard = message;
                        score = score + 2;
                    }else if(message.startsWith("GAME_OVER")){
                        pointMap.put(this, this.score);
                        pointMap.put(opponent, opponent.score);
                        OverGame(id);
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

        private void closeConnections() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                if (opponent != null && opponent.socket != null && !opponent.socket.isClosed()) {
                    opponent.socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void logGameTermination(String gameId, String player1Name, String player2Name) {
            try (FileWriter writer = new FileWriter("server.log", true)) {
                writer.write(LocalDateTime.now() + ": Game " + gameId + " terminated. Players: "
                        + player1Name + ", " + player2Name + " have left the game.\n");
            } catch (IOException e) {
                System.err.println("Failed to write log: " + e.getMessage());
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
