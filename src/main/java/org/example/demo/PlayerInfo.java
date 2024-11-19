package org.example.demo;

public class PlayerInfo {
    private String name;
    private String boardsize;
    private String status;
    private int score;
    private String gameId;

    public PlayerInfo(String name, String boardsize, String status, int score, String gameId) {
        this.name = name;
        this.boardsize = boardsize;
        this.status = status;
        this.score = score;
        this.gameId = gameId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getBoardSize(){return boardsize;}
    public void setBoardsize(String boardsize) {this.boardsize = boardsize;}

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", boardsize='" + boardsize + '\'' +
                ", status='" + status + '\'' +
                ", score=" + score +
                ", gameId='" + gameId + '\'' +
                '}';
    }
}
