package no.kristiania.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


class UserDaoTest {

    private UserDao userDao;
    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();
        userDao = new UserDao(dataSource);
    }

    @Test
    void shouldListInsertedUsers() throws SQLException {
        User user1 = exampleUser();
        User user2 = exampleUser();
        userDao.insert(user1);
        userDao.insert(user2);
        assertThat(userDao.list())
                .extracting(user -> user.getFirstName())
                .contains(user1.getFirstName(), user2.getFirstName());
    }

    @Test
    void shouldRetrieveAllUserProperties() throws SQLException {
        userDao.insert(exampleUser());
        userDao.insert(exampleUser());
        User user = exampleUser();
        userDao.insert(user);
        assertThat(user).hasNoNullFieldsOrProperties();
        assertThat(userDao.retrieve(user.getId()))
                .usingRecursiveComparison()
                .isEqualTo(user);
    }

    private User exampleUser() {
        User user = new User();
        user.setFirstName(exampleUserFirstName());
        user.setLastName(exampleUserLastName());
        user.setEmailAddress(exampleUserEmailAddress());
        return user;
    }

    private String exampleUserFirstName() {
        String[] firstNameOptions = {"Ola", "Hans", "Petter", "Silje"};
        return firstNameOptions[random.nextInt(firstNameOptions.length)];
    }
    private String exampleUserLastName() {
        String[] lastNameOptions = {"Nordmann", "Fjell", "Stein", "Skogen"};
        return lastNameOptions[random.nextInt(lastNameOptions.length)];
    }
    private String exampleUserEmailAddress() {
        String[] emailOptions = {"minepost@test.no", "enepost@mail.com", "epost1@test.no", "email@epost.no"};
        return emailOptions[random.nextInt(emailOptions.length)];
    }
}