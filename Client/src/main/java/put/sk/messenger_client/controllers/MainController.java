package put.sk.messenger_client.controllers;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import put.sk.messenger_client.App;
import put.sk.messenger_client.protocols.*;
import put.sk.messenger_client.sockets.SocketListenerThread;
import put.sk.messenger_client.sockets.SocketSenderThread;
import put.sk.messenger_client.sockets.SocketService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MainController {

    // main
    @FXML
    private Button button_send;
    @FXML
    private TextField tf_message;
    @FXML
    private Button button_logout;
    @FXML
    private Label l_friend_name;
    @FXML
    private Label l_login_text;
    @FXML
    private Label l_account_name;
    @FXML
    private ListView<String> lst_friends_list;
    @FXML
    private TextArea ta_message;

    // login
    @FXML
    private Label l_app_name;
    @FXML
    private Label l_login;
    @FXML
    private Label l_password;
    @FXML
    private TextField tf_login;
    @FXML
    private PasswordField pf_password;
    @FXML
    private Button button_sign_in;
    @FXML
    private AnchorPane ap_main;
    @FXML
    private AnchorPane ap_login;
    @FXML
    private Button button_add_friend;
    @FXML
    private TextField tf_friend;

    private final SocketService socketService;
    private SocketListenerThread socketListenerThread;
    private SocketSenderThread socketSenderThread;
    private ExecutorService executor;
    private int signInResponse;
    private String current_user;
    private String receiver;
    private ArrayList<String> friendList = new ArrayList<String>();
    private HashMap<String, ArrayList<Message>> messages = new HashMap<String, ArrayList<Message>>();
    private HashMap<String, Integer> requests = new HashMap<String, Integer>();


    private final SimpleStringProperty lastMessage;
    private final Queue<String> queue = new ArrayBlockingQueue<>(32, false);
    private final MessageService messageService = new MessageService();
    private String address;
    private int port;

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MainController() {
        System.out.println(this.address + " " + this.port);
        socketService = new SocketService("127.0.0.1", 1234);
        lastMessage = new SimpleStringProperty();
        executor = Executors.newCachedThreadPool();

        lastMessage.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                System.out.println("I have received sth");
                if(ap_login.isVisible()){
                    onConfirmationReceived(newValue);
                }
                else{
                    try {
                        System.out.println("NewValue: " + newValue.charAt(0));
                        System.out.println(newValue);
                        if(newValue.charAt(0) == 'r'){
                            System.out.println("Received the request answer");
                            requests.put(newValue.substring(6), (int) newValue.charAt(1));
                        }
                        else{
                            onMessageReceived(newValue);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        try {
            socketService.init();

            socketListenerThread = new SocketListenerThread(socketService, lastMessage);
            socketListenerThread.start();
            socketSenderThread = new SocketSenderThread(socketService, queue);
            socketSenderThread.start();

            executor.submit(socketListenerThread);
            executor.submit(socketSenderThread);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSendMsgButtonClick() throws IOException, InterruptedException {sendMessage();}

    @FXML
    public void onEnter(KeyEvent key) throws IOException {
        if (key.getCode().equals(KeyCode.ENTER)){
            sendMessage();
        }
    }

    @FXML
    private void onAddFriend() throws IOException, InterruptedException {sendFriendRequest();}

    @FXML
    public void onEnterFriend(KeyEvent key) throws IOException, InterruptedException {
        if (key.getCode().equals(KeyCode.ENTER)){
            sendFriendRequest();
        }
    }

    private String messagesToString(String receiver){
        StringBuilder output = new StringBuilder();
        for (Message message : messages.get(receiver)){
            if(Objects.equals(message.getSender(), current_user)){
                output.append("You").append(": ").append(message.getContent());
            }
            else{
                output.append(message.getSender()).append(": ").append(message.getContent()).append('\n');
            }
        }
        return String.valueOf(output);
    }

    public void sendFriendRequest() throws IOException, InterruptedException {

        FriendRequestService friendRequestService = new FriendRequestService();
        final String requestToSend = friendRequestService.encode(tf_friend.getText());

        try {
            queue.add(requestToSend);
        } catch (Exception e) {
            System.out.println("Cannot send message - queue full!");
            e.printStackTrace();
        }

        TimeUnit.SECONDS.sleep(1);

        System.out.println(requests.toString());

        System.out.println("RQG: " + requests.get(tf_friend.getText()));

        if(requests.get(tf_friend.getText()) == (int)'1'){
            final String friendToAdd = tf_friend.getText();
            if(!friendList.contains(friendToAdd)){
                lst_friends_list.getItems().add(friendToAdd);
                friendList.add(friendToAdd);
                messages.put(friendToAdd, new ArrayList<Message>());
            }
        }else{
            Alert alert = new Alert(Alert.AlertType.WARNING, "This user does not exist.", ButtonType.OK);
            alert.showAndWait();
        }


        tf_friend.setText(null);
    }

    private void sendMessage() throws IOException {

        if(tf_message.getText() != null){

            final String msgToSend = tf_message.getText() + '\n';
            Message message = new Message(this.current_user, this.receiver, msgToSend);
            String encoded_message = messageService.encode(message);

            try {
                queue.add(encoded_message);
                messages.get(receiver).add(message);
            } catch (Exception e) {
                System.out.println("Cannot send message - queue full!");

                e.printStackTrace();
            }

            Platform.runLater(()->ta_message.appendText("You: " + msgToSend));
            tf_message.setText(null);
        }
    }

    @FXML
    private void onStopButtonClick() {
        if (executor != null && socketListenerThread != null) {
            System.out.println("Stopping threads!");

            socketListenerThread.stop();
            socketSenderThread.stop();

            executor.shutdown();
        }
    }

    private void onMessageReceived(final String message) throws InterruptedException {
        // System.out.println("I have received a message: " + message);
        Message message_decoded = messageService.decode(message.getBytes());
        if(!friendList.contains(message_decoded.getSender())) {
            Platform.runLater(()->lst_friends_list.getItems().add(message_decoded.getSender()));
            friendList.add(message_decoded.getSender());
            messages.put(message_decoded.getSender(), new ArrayList<Message>());
        }
        messages.get(message_decoded.getSender()).add(message_decoded);
        System.out.println(messages.toString());

        if (Objects.equals(receiver, message_decoded.getSender())){
            Platform.runLater(()->ta_message.appendText(message_decoded.getSender() + ": " + message_decoded.getContent()));
        }
        // ta_message.appendText(message_decoded.getSender() + ": " + message_decoded.getContent() + '\n');
    }

    @FXML
    public void signIn(MouseEvent event) throws IOException, InterruptedException {
        CredentialsService credentialsService = new CredentialsService();
        Credentials credentials = new Credentials(tf_login.getText(), pf_password.getText());
        this.current_user = tf_login.getText();
        l_account_name.setText(tf_login.getText());
        final String msgToSend = credentialsService.encode(credentials, false);

        try {
            queue.add(msgToSend);
        } catch (Exception e) {
            System.out.println("Cannot send message - queue full!");

            e.printStackTrace();
        }
        TimeUnit.SECONDS.sleep(1);
        if (signInResponse == 2){
            System.out.println("signInSuccessful");
            ap_login.setVisible(false);
            ap_main.setVisible(true);
            ta_message.setEditable(false);
            tf_message.setPromptText("Enter your message here");
            tf_message.getParent().requestFocus();
            lst_friends_list.getItems().add(this.current_user);
            friendList.add(this.current_user);
            lst_friends_list.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    receiver = newValue;
                    l_friend_name.setText(newValue);
                    ta_message.setText(messagesToString(newValue));
                    System.out.println(messages.get(receiver).toString());
                    // ta_message.setScrollTop(Double.MIN_VALUE);
                }
            });
            messages.put(this.current_user, new ArrayList<Message>());
            lst_friends_list.getSelectionModel().select(this.current_user);
            System.out.println(messages.toString());
        }
        else if (signInResponse == 1){
            Alert alert = new Alert(Alert.AlertType.WARNING, "Wrong password!", ButtonType.OK);
            Optional<ButtonType> result = alert.showAndWait();
        }
        else if (signInResponse == 0){
            Alert alert = new Alert(Alert.AlertType.WARNING, "Account does not exist. Create account?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == ButtonType.YES){
                final String registerToSend = credentialsService.encode(credentials, true);

                try {
                    queue.add(registerToSend);
                } catch (Exception e) {
                    System.out.println("Cannot send message - queue full!");

                    e.printStackTrace();
                }
                TimeUnit.SECONDS.sleep(1);
                if(signInResponse == 2){
                    System.out.println("signInSuccessful");
                    ap_login.setVisible(false);
                    ap_main.setVisible(true);
                    ta_message.setEditable(false);
                    tf_message.setPromptText("Enter your message here");
                    tf_message.getParent().requestFocus();
                    lst_friends_list.getItems().add(this.current_user);
                    friendList.add(this.current_user);
                    lst_friends_list.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                        @Override
                        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                            receiver = newValue;
                            l_friend_name.setText(newValue);
                            ta_message.setText(messagesToString(newValue));
                            System.out.println(messages.get(receiver).toString());
                            // ta_message.setScrollTop(Double.MIN_VALUE);
                        }
                    });
                    messages.put(this.current_user, new ArrayList<Message>());
                    lst_friends_list.getSelectionModel().select(this.current_user);
                    System.out.println(messages.toString());
                }
            }
        }

    }

    private void onConfirmationReceived(final String message) {
        System.out.println("Return: " + message);
        if (Objects.equals(message, "0")) {
            this.signInResponse = 0;
        }
        else if (Objects.equals(message, "1")) {
            this.signInResponse = 1;
        }
        else if (Objects.equals(message, "2")) {
            this.signInResponse = 2;
        }
        else {
            System.out.println(message);
        }
    }
}