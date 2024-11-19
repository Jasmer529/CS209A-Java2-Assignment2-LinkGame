package org.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SetUp {

    @FXML
    private TextField rowInput;

    @FXML
    private TextField colInput;

    @FXML
    private TextField NAME;

    @FXML
    private Button confirmButton;  // 确认按钮

    @FXML
    private Label errorMessageLabel;  // 显示错误信息的Label

    private Application application;

    // 设置Application实例，方便调用startGame
    public void setApplication(Application application) {
        this.application = application;
    }

    @FXML
    private void handleConfirm() {
        try {
            int rows = Integer.parseInt(rowInput.getText());
            int cols = Integer.parseInt(colInput.getText());
            String name = NAME.getText();

            // 判断行列数的乘积是否为奇数
            if (rows > 0 && cols > 0) {
                if ((rows * cols) % 2 != 0) {
                    // 如果乘积是奇数，禁用Confirm按钮并显示提示信息
                    errorMessageLabel.setText("Rows * Columns cannot be odd. Please re-enter.");
                    errorMessageLabel.setStyle("-fx-text-fill: red;"); // 设置红色字体
                    return;  // 不再继续处理
                } else {
                    // 如果条件满足，启动游戏
                    System.out.println("Rows: " + rows + ", Columns: " + cols);

                    // 调用 startGame 方法，处理可能抛出的IOException
                    try {
                        application.startGame(rows + 2, cols + 2, name); // 可能抛出IOException
                    } catch (IOException e) {
                        showAlert("Error starting the game: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // 关闭当前窗口
                    Stage stage = (Stage) rowInput.getScene().getWindow();
                    stage.close();
                }
            } else {
                showAlert("Please enter positive numbers for rows and columns.");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid input! Please enter integers.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
