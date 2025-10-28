package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import library.Main;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML private Label        headerLabel;
    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;

    private String selectedRole;

    public void setRole(String role) {
        this.selectedRole = role;
        String role_name = switch (role.toLowerCase()) {
            case "student", "staff" -> "Student/Staff";
            case "author" -> "Author";
            case "librarian" -> "Librarian";
            default -> "";
        };
        headerLabel.setText(role_name + " Login");
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent home = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Home.fxml")));
        Main.getPrimaryStage().setScene(new Scene(home, 640, 480));
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String fxml;
        switch (selectedRole.toLowerCase()) {
            case "student":   fxml = "/fxml/StudentDashboard.fxml";   break;
            case "author":    fxml = "/fxml/AuthorDashboard.fxml";    break;
            case "librarian": fxml = "/fxml/LibrarianDashboard.fxml"; break;
            default:          fxml = "/fxml/Home.fxml";               break;
        }
        try {
            Parent dash = FXMLLoader.load(getClass().getResource(fxml));
            Main.getPrimaryStage().setScene(new Scene(dash, 1000, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** New: navigate to the standalone Register screen */
    @FXML
    private void handleGoToRegister(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
        Parent root = loader.load();
        RegisterController ctrl = loader.getController();
        ctrl.setRole(selectedRole);
        Main.getPrimaryStage().setScene(new Scene(root, 640, 480));
    }



    private String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
