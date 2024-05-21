package ru.galeev;

public interface AuthenticationService {
    String getNicknameByLoginAndPassword(String login, String password);

    boolean register(String login, String password, String nickname);

    boolean isLoginAlreadyExist(String login);

    boolean isNicknameAlreadyExist(String nickname);

    void addRole(String nickname, String role);

    void removeRole(String nickname, String role);

    boolean hasRole(String nickname, String role);

    String setNickname(String nickname, String changeNick);

}
