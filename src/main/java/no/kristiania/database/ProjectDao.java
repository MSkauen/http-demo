package no.kristiania.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDao extends AbstractDao<Project> {

    public ProjectDao(DataSource dataSource) {
        super(dataSource);
    }

    public void insert(Project project) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO projects (project_name, project_color, project_status) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, project.getName());
                statement.setString(2, project.getColor());
                statement.setString(3, project.getStatus());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    generatedKeys.next();
                    project.setId(generatedKeys.getLong("id"));
                }
            }
        }
    }

    public Project retrieve(Long id) throws SQLException {
        return retrieve(id, "SELECT * FROM projects WHERE id = ?");
    }

    public List<Project> list() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM projects")) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<Project> projects = new ArrayList<>();
                    while (rs.next()) {
                        projects.add(mapRow(rs));
                    }
                    return projects;
                }
            }
        }
    }

    @Override
    protected Project mapRow(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getLong("id"));
        project.setName(rs.getString("project_name"));
        project.setColor(rs.getString("project_color"));
        project.setStatus(rs.getString("project_status"));
        return project;
    }
}
