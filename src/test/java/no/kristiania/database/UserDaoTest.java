package no.kristiania.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


class UserDaoTest {
    @Test
    void shouldListInsertedUsers() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();

        UserDao userDao = new UserDao(dataSource);
        String user = exampleUser();
        userDao.insert(user);
        assertThat(userDao.list()).contains(user);
        System.out.println(userDao.list());
    }

    private String exampleUser() {
        String[] firstNameOptions = {"Ola", "Hans", "Petter", "Silje"};
        String[] lastNameOptions = {"Nordmann", "Fjell", "Stein", "Skogen"};
        String[] emailOptions = {"minepost@test.no", "enepost@mail.com", "epost1@test.no", "email@epost.no"};
        Random random = new Random();
        return firstNameOptions[random.nextInt(firstNameOptions.length)]
                + "," + lastNameOptions[random.nextInt(lastNameOptions.length)]
                + "," + emailOptions[random.nextInt(emailOptions.length)];

    }
}