package no.kristiania.http;

import no.kristiania.database.Project;
import no.kristiania.database.ProjectDao;

import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ProjectPostController implements HttpController {

    private ProjectDao projectDao;

    public ProjectPostController(ProjectDao projectDao) {

        this.projectDao = projectDao;
    }

    @Override
    public void handle(HttpMessage request, Socket clientSocket) throws IOException, SQLException {

        QueryString requestParameters = new QueryString(request.getBody());

        Project project = new Project();
        String projectNameDecoded = URLDecoder.decode(requestParameters.getParameter("project_name"), StandardCharsets.UTF_8);
        String projectColorDecoded = URLDecoder.decode(requestParameters.getParameter("project_color"), StandardCharsets.UTF_8);
        String projectStatusDecoded = URLDecoder.decode(requestParameters.getParameter("project_status"), StandardCharsets.UTF_8);

        project.setColor(projectColorDecoded);
        project.setName(projectNameDecoded);
        project.setStatus(projectStatusDecoded);

        projectDao.insert(project);

        String body = "Ok";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }
}
