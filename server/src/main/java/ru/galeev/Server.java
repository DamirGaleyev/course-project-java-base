package ru.galeev;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationService authenticationService;

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
    private Socket socket;

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.authenticationService = new JDBSUserAuthenticationService();
            JDBSUserAuthenticationService.connect();
            System.out.println("Сервис аутентификации запущен: " + authenticationService.getClass().getSimpleName());
            System.out.printf("Сервер запущен на порту: %d, ожидаем подключения клиентов\n", port);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(this, socket);
                } catch (Exception e) {
                    System.out.println("Возникла ошибка при обработке подключившегося клиента");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("К чату присоединился " + clientHandler.getNickname());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Из чата вышел " + clientHandler.getNickname());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized boolean isNicknameBusy(String nickname) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    public void sendMessageByNickName(ClientHandler sender, String recipient, String message) {
        for (ClientHandler c : clients) {
            if (recipient.equals(c.getNickname())) {
                c.sendMessage(sender.getNickname() + ": " + message);
                sender.sendMessage(sender.getNickname() + ": " + message);
            }
        }
    }

    public void shutdownMethod() throws IOException {
        socket.close();
    }

    public synchronized void unsubscribe(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getNickname().equals(nickname)) {
                try {
                    clients.get(i).disconnect();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized List<String> showActivelist() {
        List<String> listForShow = new ArrayList<>();
        for (ClientHandler c : clients) {
            listForShow.add(c.getNickname());
        }
        return listForShow;
    }
}
