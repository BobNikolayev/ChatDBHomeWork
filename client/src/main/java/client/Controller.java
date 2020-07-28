package client;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clientList;

    private final int PORT = 8189;
    private final String IP_ADDRESS = "localhost";
    private final String CHAT_TITLE_EMPTY = "Chat july 2020";


    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nick;
    private String login;

    private Stage stage;
    private Stage regStage;
    RegController regController;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        if (!authenticated) {
            nick = "";
        }
        setTitle(nick);
        textArea.clear();
        historyPrint();
    }
//метод создания файла
    public String historyCreator(){
        FileOutputStream historyOut = null;

        String ini = "ini";

        String log = loginField.getText().trim();

        File history = new File("history_" + log + ".txt");
        if(history.length() != 0){
        return login = loginField.getText().trim();
        }else{
            try {

                history.createNewFile();
                historyOut = new FileOutputStream("history_" + login + ".txt");
                historyOut.write(ini.getBytes());
                return login = loginField.getText().trim();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return login;
    }
//метод сохранения сообщений
    public void messageSave(String msg){
        FileWriter historyWriter  = null;

        try {
            historyWriter = new FileWriter("history_" + login + ".txt", true);

            historyWriter.write(msg + " ");

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                historyWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
//метод выведения истории
    public void historyPrint(){
//        FileInputStream fis = null;
//        InputStreamReader isr = null;


        try {
//            fis = new FileInputStream("history_" + login + ".txt");
//            isr = new InputStreamReader(fis,"UTF-8");
            Path file = Paths.get("history_" + login + ".txt");
            List<String> lines = Files.readAllLines(file);



            textArea.appendText(String.valueOf(lines));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bye");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });

        setAuthenticated(false);

        regStage = createRegWindow();
    }


    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/authok ")) {
                            historyCreator();
                            nick = str.split("\\s")[1];
//                            historyCreator();
                            setAuthenticated(true);
                            break;
                        }

                        if (str.equals("/end")) {
                            throw new RuntimeException("Сервер нас отключил по таймауту");
                        }

                        if (str.startsWith("/regresult ")) {
                            String result = str.split("\\s")[1];
                            if (result.equals("ok")) {
                                regController.addMessage("Регистрация прошла успешно");
                            } else {
                                regController.addMessage("Регистрация не получилась, возможно логин или никнейм заняты");
                            }
                        }

                        textArea.appendText(str + "\n");

                    }


                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                messageSave(textArea.getText());
                                setAuthenticated(false);
                                break;
                            }
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }

                        } else {
                            textArea.appendText(str + "\n");

                        }
                    }
                }catch (RuntimeException e)   {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            textField.requestFocus();
            textField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim()));
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nick) {
        Platform.runLater(() -> {
            stage.setTitle(CHAT_TITLE_EMPTY + " : " + nick);
        });
    }

    public void clickClientList(MouseEvent mouseEvent) {
        System.out.println(clientList.getSelectionModel().getSelectedItem());
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("/w %s ", receiver));
        if (mouseEvent.isAltDown()) {
            System.out.println("AltDown");
        }
    }

    private Stage createRegWindow() {
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            stage.setTitle("Chat reg window");
            stage.setScene(new Scene(root, 350, 250));
            stage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    public void showRegWindow(ActionEvent actionEvent) {
        regStage.show();
    }

    public void tryToReg(String login, String password, String nickname) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(String.format("/reg %s %s %s", login, password, nickname));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
