package no.kristiania.http;

import no.kristiania.database.Member;
import no.kristiania.database.MemberDao;
import no.kristiania.database.ProjectDao;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private Map<String, HttpController> controllers;

    private MemberDao memberDao;
    private final ServerSocket serverSocket;

    public HttpServer(int port, DataSource dataSource) throws IOException {
        memberDao = new MemberDao(dataSource);
        ProjectDao projectDao = new ProjectDao(dataSource);
        controllers = Map.of(
                "/api/projects/newProject", new ProjectPostController(projectDao),
                "/api/projects/listProjects", new ProjectGetController(projectDao)
        );

        serverSocket = new ServerSocket(port);
        logger.warn("Server started on port {}", serverSocket.getLocalPort());

        System.out.println("Server running on port: " + serverSocket.getLocalPort()
                + "\r\n Access server using any IP-Address:" + serverSocket.getLocalPort() + ", e.g 127.0.0.1:"
                + serverSocket.getLocalPort() + " or localhost:" + serverSocket.getLocalPort());
        new Thread(() -> {
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handleRequest(Socket clientSocket) throws IOException, SQLException {
        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println(requestLine);

        String requestMethod = requestLine.split(" ")[0];
        String requestTarget = requestLine.split(" ")[1];

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            if (requestPath.equals("/api/members/newMember")) {
                handlePostMember(clientSocket, request);
            } else {
               getController(requestPath).handle(request, clientSocket);
            }
        } else {
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else if (requestPath.equals("/")){
                handleFileRequest(clientSocket, "/index.html");
            } else if (requestPath.equals("/api/members/listMembers")) {
                handleGetMembers(clientSocket);
            } else {
                HttpController controller = controllers.get(requestPath);
                if (controller != null) {
                    controller.handle(request, clientSocket);
                } else {
                    handleFileRequest(clientSocket, requestPath);
                }
            }
        }

    }

    private HttpController getController(String requestPath) {
        return controllers.get(requestPath);
    }

    private void handlePostMember(Socket clientSocket, HttpMessage request) throws SQLException, IOException {
        QueryString requestParameters = new QueryString(request.getBody());

        String firstName = requestParameters.getParameter("first_name");
        String lastName = requestParameters.getParameter("last_name");
        String emailAddress = requestParameters.getParameter("email_address");

        if (firstName != null | lastName != null |  emailAddress != null) {
            String firstNameDecoded = URLDecoder.decode(firstName, StandardCharsets.UTF_8);
            String lastNameDecoded = URLDecoder.decode(lastName, StandardCharsets.UTF_8);
            String emailAddressDecoded = URLDecoder.decode(emailAddress, StandardCharsets.UTF_8);

            Member member = new Member();
            member.setFirstName(firstNameDecoded);
            member.setLastName(lastNameDecoded);
            member.setEmailAddress(emailAddressDecoded);
            memberDao.insert(member);
        }

        String body = "Ok";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }

    private void handleFileRequest(Socket clientSocket, String requestPath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(requestPath)) {
            if (inputStream == null) {
                String body = requestPath + " does not exist";
                String response = "HTTP/1.1 404 Not found\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;

                clientSocket.getOutputStream().write(response.getBytes());
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            inputStream.transferTo(buffer);

            String contentType = "text/plain";
            if (requestPath.endsWith(".html")) {
                contentType = "text/html";
            }
            if (requestPath.endsWith(".css")) {
                contentType = "text/css";
            }
            if (requestPath.endsWith(".png")){
                contentType = "image/png";
            } else if (requestPath.endsWith(".ico")){
                contentType = "image/x-icon";
            }

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + buffer.toByteArray().length + "\r\n" +
                    "Connection: close\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";
            clientSocket.getOutputStream().write(response.getBytes());
            clientSocket.getOutputStream().write(buffer.toByteArray());
        }
    }

    private void handleGetMembers(Socket clientSocket) throws IOException, SQLException {
        String body = "<ul>";

        for (Member member : memberDao.list()) {
            body += "<li>" + member.getFirstName() + " " + member.getLastName() + " " + member.getEmailAddress() + "</li>";
        }

        body += "</ul>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
    }

    private void handleEchoRequest(Socket clientSocket, String requestTarget, int questionPos) throws IOException {
        String statusCode = "200";
        String body = "Hello World";
        if (questionPos != -1) {
            QueryString queryString = new QueryString(requestTarget.substring(questionPos + 1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
        clientSocket.close();
    }

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader("./pgr203.properties")) {
            properties.load(fileReader);
        } catch (Exception e) {
            logger.error("pgr203.properties file missing!");
            File contentRoot = new File("./");
            System.out.println("Please create pgr203.properties file in" + contentRoot.getAbsolutePath() + " and assign database url, username and password {}");
        }

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(properties.getProperty("dataSource.url"));
        dataSource.setUser(properties.getProperty("dataSource.username"));
        dataSource.setPassword(properties.getProperty("dataSource.password"));

        logger.info("Using database: {}", dataSource.getUrl());
        Flyway.configure().dataSource(dataSource).load().migrate();

        HttpServer server = new HttpServer(8080, dataSource);
    }

    public List<Member> getMembers() throws SQLException {
        return memberDao.list();
    }
}