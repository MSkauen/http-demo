package no.kristiania.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public void insert(String user) throws SQLException {
        String firstName = user.split(",")[0];
        String lastName = user.split(",")[1];
        String email = user.split(",")[2];

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users (first_name, last_name, email) values (?, ?, ?)")) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, email);

                statement.executeUpdate();
            }
        }
    }

    public List<String> list() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users")) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<String> users = new ArrayList<>();
                    while (rs.next()) {
                        users.add(rs.getString("first_name") + "," + rs.getString("last_name")
                                + "," + rs.getString("email"));
                    }
                    return users;
                }
            }
        }
    }
}