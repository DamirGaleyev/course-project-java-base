package ru.galeev;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBSUserAuthenticationService implements AuthenticationService {


    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5433/DatabaseOfUsers";
    private static final String USERS_QUERY = "SELECT * FROM users";
    private static final String USER_ROLES_QUERY = """
            SELECT r.id as id, r.role as name FROM user_to_role ur
            JOIN roles r ON  r.id = ur.role_id
            WHERE ur.user_id = ?
            """;

    private static final String USERS_ADD_QUERY0 = "UPDATE users SET nickname = ? WHERE ID= ? ";
    private static final String USERS_ADD_QUERY1 = "INSERT INTO users (id, login, password, nickname) VALUES (?,?,?,?) ";
    private static final String USERS_ADD_QUERY2 = "INSERT INTO user_to_role (user_id, role_id) VALUES (?,?) ";
    private static final String USERS_ADD_QUERY3 = "DELETE FROM user_to_role WHERE user_id = ? AND role_id = ? ";

    public static List<User> users;

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.getLogin().equals(login) && u.getPassword().equals(password)) {
                System.out.println(u.getNickname());
                return u.getNickname();
            }
        }
        return null;
    }

    @Override
    public boolean register(String login, String password, String nickname) {
        if (isLoginAlreadyExist(login)) {
            return false;
        }
        if (isNicknameAlreadyExist(nickname)) {
            return false;
        }
        int idNewUser = users.size();
        int id = idNewUser + 1;

        users.add(new User(login, password, nickname));
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "123")) {
            PreparedStatement ps = connection.prepareStatement(USERS_ADD_QUERY1);
            PreparedStatement ps2 = connection.prepareStatement(USERS_ADD_QUERY2);
            ps.setInt(1, id);
            ps.setString(2, login);
            ps.setString(3, password);
            ps.setString(4, nickname);
            ps2.setInt(1, id);
            ps2.setInt(2, 2);
            ps.executeUpdate();
            ps2.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNicknameAlreadyExist(String nickname) {
        for (User u : users) {
            if (u.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addRole(String nickname, String role) {
        for (User user : users) {
            if (user.getNickname().equals(nickname)) {
                int userIDinDB = user.getId();
                try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "123")) {
                    PreparedStatement ps2 = connection.prepareStatement(USERS_ADD_QUERY2);
                    if (role.equalsIgnoreCase("Admin")) {
                        user.getRoles().add(new RoleInDatabase(1, role));
                        ps2.setInt(1, userIDinDB);
                        ps2.setInt(2, 1);
                        ps2.executeUpdate();
                    } else if (role.equalsIgnoreCase("User")) {
                        user.getRoles().add(new RoleInDatabase(2, role));
                        ps2.setInt(1, userIDinDB);
                        ps2.setInt(2, 1);
                        ps2.executeUpdate();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void removeRole(String nickname, String role) {
        for (User user : users) {
            if (user.getNickname().equals(nickname)) {
                int userIDinDB = user.getId();
                try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "123")) {
                    PreparedStatement ps2 = connection.prepareStatement(USERS_ADD_QUERY3);
                    if (role.equalsIgnoreCase("Admin")) {
                        user.getRoles().remove(1);
                        ps2.setInt(1, userIDinDB);
                        ps2.setInt(2, 1);
                        ps2.executeUpdate();
                    } else if (role.equalsIgnoreCase("User")) {
                        user.getRoles().remove(1);
                        ps2.setInt(1, userIDinDB);
                        ps2.setInt(2, 2);
                        ps2.executeUpdate();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    @Override
    public boolean hasRole(String nickname, String role) {
        List<RoleInDatabase> rolesOfUsers;
        for (User user : users) {
            rolesOfUsers = user.getRoles();
            if (user.getNickname().equals(nickname)) {
                for (RoleInDatabase rolesOfUser : rolesOfUsers) {
                    if (rolesOfUser.getName().equalsIgnoreCase(role))
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public String setNickname(String nickname, String changeNick) {
        for (User user : users) {
            if (user.getNickname().equals(nickname)) {
                int userId = user.getId();
                user.setNickname(changeNick);
                try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "123")) {
                    PreparedStatement ps = connection.prepareStatement(USERS_ADD_QUERY0);
                    ps.setString(1, changeNick);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return user.getNickname();
            }
        }
        return null;
    }



    public static void connect() {
        users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "123")) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet usersResultSet = statement.executeQuery(USERS_QUERY)) {     // получаем таблицу
                    while (usersResultSet.next()) {                                        // двигаемся по строчкам таблицы. Если next возвращает false, то тогда мы дошли до конца таблицы
                        int id = usersResultSet.getInt("id"); // извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                        String login = usersResultSet.getString(2); // извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                        String password = usersResultSet.getString(3); // извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                        String nickname = usersResultSet.getString(4);// извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                        User user = new User(id, login, password, nickname); // создаем объект класса, с извлеченными значениями таблицы в виде переменных
                        users.add(user); // добавляем объект в список.
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(USER_ROLES_QUERY)) {
                    for (User user : users) {
                        List<RoleInDatabase> roles = new ArrayList<>();
                        ps.setInt(1, user.getId());
                        try (ResultSet usersResultSet = ps.executeQuery()) {     // получаем таблицу
                            while (usersResultSet.next()) {                                        // двигаемся по строчкам таблицы. Если next возвращает false, то тогда мы дошли до конца страницы
                                int id = usersResultSet.getInt("id"); // извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                                String name = usersResultSet.getString("name"); // извлекаем соответствующую колонку таблицы БД в переменную с соответствующим названием
                                RoleInDatabase roleInDatabase = new RoleInDatabase(id, name); // создаем объект класса, с извлеченными значениями таблицы в виде переменных
                                roles.add(roleInDatabase); // добавляем объект в список.
                            }
                            user.setRoles(roles);
                        }
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        System.out.print(users);
    }
}