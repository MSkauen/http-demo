package no.kristiania.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectDaoTest {

    private ProjectDao projectDao;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();
        projectDao = new ProjectDao(dataSource);
    }
    @Test
    void shouldListAllProjects() throws SQLException {
        Project project1 = exampleProject();
        Project project2 = exampleProject();
        projectDao.insert(project1);
        projectDao.insert(project2);
        assertThat(projectDao.list())
                .extracting(Project::getName)
                .contains(project1.getName(), project2.getName());
    }

    @Test
    void shouldRetrieveAllProjectProperties() {

    }
    private Project exampleProject() {
        return null;
    }
}
