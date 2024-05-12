package ru.galeev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;

    public AtomicLong startNonActiveTime = new AtomicLong();

    Thread nonActiveTimerTask = new Thread() {
        public void run() {
            while (true) {
                LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startNonActiveTime.get()), ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.now();
                Duration difference = Duration.between(start, end);
                long minutes = difference.toMinutes();

                if (minutes > 20) {
                    server.broadcastMessage("Пользователь " + nickname + " вышел по лимиту времени наконец то");
                    server.unsubscribe(ClientHandler.this);
                    break;
                }
            }
        }
    };

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        startNonActiveTime.set(new Date().getTime());
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                if (tryToAuthenticate()) {
                    System.out.println("После подключения" + JDBSUserAuthenticationService.users);
                    communicate();
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void communicate() throws IOException, ParseException {
        nonActiveTimerTask.start();
        while (true) {
            startNonActiveTime.set(new Date().getTime());
            String msg = in.readUTF();

            if (msg.startsWith("/")) {
                if (msg.startsWith("/exit")) {
                    break;
                }
                if (msg.startsWith("/w ")) {
                    String[] tokens = msg.split(" ", 3);
                    if (tokens.length != 3) {
                        sendMessage("Некорректный формат запроса");
                        continue;
                    }
                    String recipient = tokens[1];
                    String message = tokens[2];
                    if (server.getAuthenticationService().isNicknameAlreadyExist(recipient)) {
                        server.sendMessageByNickName(this, recipient, message);
                    } else {
                        sendMessage("Нет такого пользователя");
                    }
                } else if (msg.startsWith("/addRole ")) {
                    if (!server.getAuthenticationService().hasRole(this.nickname, "Admin")) {
                        sendMessage("Нет прав администратора");
                        continue;
                    }
                    String[] tokens = msg.split(" ", 3);
                    String nickname = tokens[1];
                    String roleStr = tokens[2];
                    Role r = Role.fromValue(roleStr);
                    if (r == null) {
                        sendMessage("такая роль не найдена");
                        continue;
                    }
                    if (server.getAuthenticationService().hasRole(nickname, roleStr)) {
                        sendMessage("Вы патаетесь добавить существующую роль");
                        continue;
                    }
                    server.getAuthenticationService().addRole(nickname, roleStr);
                    System.out.print(JDBSUserAuthenticationService.users);
                    sendMessage("Роль добавлена");
                } else if (msg.startsWith("/removeRole ")) {
                    if (!server.getAuthenticationService().hasRole(this.nickname, "Admin")) {
                        sendMessage("Нет прав администратора");
                        continue;
                    }
                    String[] tokens = msg.split(" ", 3);
                    String nickname = tokens[1];
                    String roleStr = tokens[2];
                    Role r = Role.fromValue(roleStr);
                    if (r == null) {
                        sendMessage("такая роль не найдена");
                        continue;
                    }
                    if (server.getAuthenticationService().hasRole(nickname, roleStr)) {
                        server.getAuthenticationService().removeRole(nickname, roleStr);
                        System.out.print(JDBSUserAuthenticationService.users);
                        sendMessage("Роль снята");
                    }
                } else if (msg.startsWith("/ban ")) {
                    if (!server.getAuthenticationService().hasRole(this.nickname, "Admin")) {
                        sendMessage("Нет прав администратора");
                        continue;
                    }
                    String[] tokens = msg.split(" ", 2);
                    String nickname = tokens[1];
                    if (!server.getAuthenticationService().isNicknameAlreadyExist(nickname)) {
                        sendMessage("Пользователь не найден");
                        continue;
                    }
                    server.unsubscribe(nickname);

                } else if (msg.startsWith("/shutdown")) {
                    if (!server.getAuthenticationService().hasRole(this.nickname, "Admin")) {
                        sendMessage("Нет прав администратора");
                        continue;
                    }
                    server.shutdownMethod();

                } else if (msg.startsWith("/changeNick ")) {
                    String[] tokens = msg.split(" ", 2);
                    if (tokens.length != 2) {
                        sendMessage("Некорректный формат запроса");
                        continue;
                    }
                    String changeNick = tokens[1];
                    String nickname = this.nickname;

                    if (server.isNicknameBusy(changeNick)) {
                        sendMessage("Указанный nickname уже занят. Выберете другое имя");
                        continue;
                    }
                    server.getAuthenticationService().setNickname(nickname, changeNick);
                    this.nickname = changeNick;
                    sendMessage(nickname + " изменил имя на " + this.nickname);
                } else if (msg.startsWith("/activelist")) {
                    String[] tokens = msg.split(" ");
                    if (tokens.length != 1) {
                        sendMessage("Некорректный формат запроса");
                        continue;
                    }
                    sendMessage(server.showActivelist().toString());
                }
                continue;
            }
            server.broadcastMessage(nickname + ": " + msg);
        }
        server.unsubscribe(this);
    }

    private boolean tryToAuthenticate() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/auth ")) {
                String[] tokens = msg.split(" ");
                if (tokens.length != 3) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = server.getAuthenticationService().getNicknameByLoginAndPassword(login, password);
                if (nickname == null) {
                    sendMessage("Неправильный логин/пароль");
                    continue;
                }
                if (server.isNicknameBusy(nickname)) {
                    sendMessage("Указанная учетная запись уже занята. Попробуйте зайти позднее");
                    continue;
                }
                this.nickname = nickname;
                server.subscribe(this);
                sendMessage(nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.startsWith("/register ")) {
                // /register login pass nickname
                String[] tokens = msg.split(" ");
                if (tokens.length != 4) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = tokens[3];
                if (server.getAuthenticationService().isLoginAlreadyExist(login)) {
                    sendMessage("Указанный логин уже занят");
                    continue;
                }
                if (server.getAuthenticationService().isNicknameAlreadyExist(nickname)) {
                    sendMessage("Указанный никнейм уже занят");
                    continue;
                }
                if (!server.getAuthenticationService().register(login, password, nickname)) {
                    sendMessage("Не удалось пройти регистрацию");
                    continue;
                }
                this.nickname = nickname;
                server.subscribe(this);
                sendMessage("Вы успешно зарегистрировались! " + nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.equals("/exit")) {
                return false;
            } else {
                sendMessage("Вам необходимо авторизоваться");
            }
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + " " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
