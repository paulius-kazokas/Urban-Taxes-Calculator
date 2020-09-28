package repositories;

import config.DatabaseConfig;
import entities.User;
import interfaces.IUserRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import security.SecurityUtils;
import utility.InputValidity;

import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static config.SystemConstants.*;

@Slf4j
public class UserRepository implements IUserRepository {

    private static final OutputStream OUT = System.out;
    private DatabaseConfig databaseConfig;

    public UserRepository(DatabaseConfig dc) {
        this.databaseConfig = dc;
    }

    @SneakyThrows
    private ResultSet resultSet(String query, String queryParam) {
        if (queryParam.isBlank()) OUT.write(String.format("Query parameter '%s' is invalid", queryParam).getBytes());
        if (query.isBlank()) OUT.write("Empty query occurred".getBytes());
        return databaseConfig.connectionToDatabase().createStatement().executeQuery(query);
    }

    @SneakyThrows
    private boolean userValidity(String param) {
        if (param.isBlank()) OUT.write("Invalid username provided".getBytes());
        return !param.isBlank() && checkIfUserExists(param);
    }

    @Override
    public Integer getUserId(String username) {

        ResultSet resultSet = resultSet(
                String.format("SELECT %s FROM %s WHERE %s = '%s'",
                        UTC_USERS_TABLE_ID,
                        UTC_USERS_TABLE,
                        UTC_USERS_TABLE_USERNAME, username)
                , username
        );

        try {
            if (resultSet.next()) {
                int id = resultSet.getInt(UTC_USERS_TABLE_ID);
                databaseConfig.closeConnection();
                return id;
            }
        } catch (SQLException sql) {
            log.error(sql.toString());
        }

        return null;
    }

    @Override
    public boolean checkIfUserExists(String username) {

        ResultSet resultSet = resultSet(
                String.format("SELECT %s FROM %s WHERE %s = '%s'",
                        UTC_USERS_TABLE_USERNAME,
                        UTC_USERS_TABLE,
                        UTC_USERS_TABLE_USERNAME, username)
                , username
        );

        try {
            if (resultSet.next()) {
                String receivedUsername = resultSet.getString(UTC_USERS_TABLE_USERNAME);
                databaseConfig.closeConnection();
                return resultSet.next() && !receivedUsername.isBlank() && receivedUsername.equals(username);
            }
        } catch (SQLException sql) {
            log.error(sql.toString());
        }

        return false;
    }

    @SneakyThrows(SQLException.class)
    @Override
    public User getUserByUsername(String username) {

        if (userValidity(username)) {

            ResultSet resultSet = resultSet(String.format("SELECT %s FROM %s WHERE %s = '%s'", SELECT_ALL, UTC_USERS_TABLE, UTC_USERS_TABLE_USERNAME, username), username);

            if (resultSet.next()) {
                // array check
                Integer userId = getUserId(username);
                String userPassword = resultSet.getString(UTC_USERS_TABLE_PASSWORD);
                String user
                User user = User.builder()
                        .id(userId)
                        .username(username)
                        .password(resultSet.getString(UTC_USERS_TABLE_PASSWORD))
                        .name(resultSet.getString(UTC_USERS_TABLE_NAME))
                        .lastname(resultSet.getString(UTC_USERS_TABLE_LASTNAME))
                        .email(resultSet.getString(UTC_USERS_TABLE_EMAIL))
                        .personalCode(resultSet.getString(UTC_USERS_TABLE_PERSONAL_CODE))
                        .build();

                databaseConfig.closeConnection();
                return user;
            }
        }

        return null;
    }

    @SneakyThrows(SQLException.class)
    @Override
    public User getUser(String personalCode) {

        ResultSet resultSet = resultSet(String.format("SELECT %s FROM %s WHERE %s = '%s'", SELECT_ALL, UTC_USERS_TABLE, UTC_USERS_TABLE_PERSONAL_CODE, personalCode), personalCode);

        if (resultSet.next()) {
            Integer userId = getUserId(resultSet.getString(UTC_USERS_TABLE_ID));
            String userUsername = resultSet.getString(UTC_USERS_TABLE_USERNAME);
            String userPassword = resultSet.getString(UTC_USERS_TABLE_PASSWORD);
            String userName = resultSet.getString(UTC_USERS_TABLE_NAME);
            String userLastname = resultSet.getString(UTC_USERS_TABLE_LASTNAME);
            String userEmail = resultSet.getString(UTC_USERS_TABLE_EMAIL);
            String userPersonalCode = resultSet.getString(UTC_USERS_TABLE_PERSONAL_CODE);
            databaseConfig.closeConnection();

            if (InputValidity.checkArrayForFalseItemValue(new String[]{userId.toString(), userUsername, userPassword, userName, userLastname, userEmail, userPersonalCode})) {
                return User.builder().id(userId).username(userUsername).password(userPassword).name(userName).lastname(userLastname).email(userEmail).personalCode(userPersonalCode).build();
            }
        }

        return null;
    }

    @Override
    public boolean checkIfPasswordMatches(String password, String userDatabasePassword, SecurityUtils securityUtils) {
        return securityUtils.sha512Hash(password).equals(userDatabasePassword);
    }

    @SneakyThrows(SQLException.class)
    @Override
    public void registerNewUser(String username, String password, String name, String lastname, String email, String personalCode) {

        String query = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)", UTC_USERS_TABLE, UTC_USERS_TABLE_USERNAME, UTC_USERS_TABLE_PASSWORD, UTC_USERS_TABLE_NAME, UTC_USERS_TABLE_LASTNAME, UTC_USERS_TABLE_EMAIL, UTC_USERS_TABLE_PERSONAL_CODE);

        PreparedStatement statement = databaseConfig.connectionToDatabase().prepareStatement(query);
        statement.setString(1, username);
        statement.setString(2, password);
        statement.setString(3, name);
        statement.setString(4, lastname);
        statement.setString(5, email);
        statement.setString(6, personalCode);

        statement.execute();
        databaseConfig.closeConnection();
    }

    @SneakyThrows(SQLException.class)
    @Override
    public void deleteUserByUsername(String username) {

        Statement statement = databaseConfig.connectionToDatabase().createStatement();
        statement.executeUpdate(String.format("DELETE FROM %s WHERE username = '%s'", UTC_USERS_TABLE, username));
        databaseConfig.closeConnection();
    }

    @SneakyThrows(SQLException.class)
    @Override
    public String getPersonalCodeByUsername(String username) {

        ResultSet resultSet = resultSet(String.format("SELECT %s FROM %s WHERE %s = '%s'", UTC_USERS_TABLE_PERSONAL_CODE, UTC_USERS_TABLE, UTC_USERS_TABLE_USERNAME, username), username);

        if (resultSet.next()) {
            String personalCode = resultSet.getString(UTC_USERS_TABLE_PERSONAL_CODE);
            databaseConfig.closeConnection();
            if (!personalCode.isBlank()) return personalCode;
        }
        return null;
    }

}
