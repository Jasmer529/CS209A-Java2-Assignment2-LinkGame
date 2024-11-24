package org.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SetUp {

    @FXML
    private TextField rowInput;

    @FXML
    private TextField colInput;

    @FXML
    private TextField NAME;

    @FXML
    private TextField PassWord;
    @FXML
    private Button confirmButton;

    @FXML
    private Label errorMessageLabel;

    private Application application;

    public void setApplication(Application application) {
        this.application = application;
    }

    @FXML
    private void handleConfirm() {
        try {
            int rows = Integer.parseInt(rowInput.getText());
            int cols = Integer.parseInt(colInput.getText());
            String name = NAME.getText();
            String pw = PassWord.getText();

            if (rows > 0 && cols > 0) {
                if ((rows * cols) % 2 != 0) {
                    errorMessageLabel.setText("Rows * Columns cannot be odd. Please re-enter.");
                    errorMessageLabel.setStyle("-fx-text-fill: red;");
                    return;
                } else {
                    if(!validateLogin(name, pw)){
                        errorMessageLabel.setText("Wrong Password.");
                        errorMessageLabel.setStyle("-fx-text-fill: red;");
                    }else {
                        System.out.println("Rows: " + rows + ", Columns: " + cols);
                        try {
                            application.startGame(rows + 2, cols + 2, name);
                        } catch (IOException e) {
                            showAlert("Error starting the game: " + e.getMessage());
                            e.printStackTrace();
                        }
                        Stage stage = (Stage) rowInput.getScene().getWindow();
                        stage.close();
                    }
                }
            } else {
                showAlert("Please enter positive numbers for rows and columns.");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid input! Please enter integers.");
        }
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
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
