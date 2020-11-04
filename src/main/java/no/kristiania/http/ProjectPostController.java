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

    @Override
    public void handle(HttpMessage request, Socket clientSocket) throws IOException {
        /*
        QueryString requestParameters = new QueryString(request.getBody());

        String projectName = requestParameters.getParameter("project_name");
        String projectNameDecoded = URLDecoder.decode(projectName, StandardCharsets.UTF_8);

        Project project = new Project();
        project.setName(projectNameDecoded);
        project.setColor(requestParameters.getParameter("project_color"));
        projectDao.insert(project);
*/
        String body = "Ok";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }
}
