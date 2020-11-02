package no.kristiania.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private DataSource dataSource;

    public UserDao(DataSource dataSource) {

        this.dataSource = dataSource;
    }

    public static void main(String[] args){
        /*
        System.out.println("MAIN USERDAO");
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/kristianiausers");
        dataSource.setUser("kristianiaroot");
        dataSource.setPassword("ecrUHxEqSv");

         */
    }

    public void insert(User user) throws SQLException {

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users (first_name, last_name, email_address) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                    )) {
                statement.setString(1, user.getFirstName());
                statement.setString(2, user.getLastName());
                statement.setString(3, user.getEmailAddress());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    generatedKeys.next();
                    user.setId(generatedKeys.getLong("id"));
                }
            }
        }
    }

    public User retrieve(Long id) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmailAddress(rs.getString("email_address"));
        return user;
    }

    public List<User> list() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users")) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<User> users = new ArrayList<>();
                    while (rs.next()) {
                        users.add(mapRowToUser(rs));
                    }
                    return users;
                }
            }
        }
    }
}