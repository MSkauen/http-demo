package no.kristiania.http;

import no.kristiania.database.UserDao;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private UserDao userDao;

    public HttpServer(int port, DataSource datasSource) throws IOException {
        userDao = new UserDao(datasSource);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server running on port: " + port + "\r\n Access server using any IP-Address:" + port + ", e.g 127.0.0.1:" + port + " or localhost:" + port);
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


    private void handleRequest(Socket clientSocket) throws IOException, SQLException {
        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println(requestLine);

        String requestMethod = requestLine.split(" ")[0];
        String requestTarget = requestLine.split(" ")[1];

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            QueryString requestParameters = new QueryString(request.getBody());
            String firstName = requestParameters.getParameter("first_name");
            String lastName = requestParameters.getParameter("last_name");
            String emailAddress = requestParameters.getParameter("email_address");

            if (firstName != null | lastName != null | emailAddress != null) {
                String requestParametersDecoded = java.net.URLDecoder.decode(firstName + "," + lastName + "," + emailAddress, StandardCharsets.UTF_8);
                userDao.insert(requestParametersDecoded);
            }

            String body = "Ok";
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    body;

            clientSocket.getOutputStream().write(response.getBytes());
        } else {
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else if (requestPath.equals("/")){
                handleFileRequest(clientSocket, "/index.html");
            } else if (requestPath.equals("/api/members")) {
                handleGetMembers(clientSocket);
            } else {
                handleFileRequest(clientSocket, requestPath);
            }
        }

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
            } else if (requestPath.endsWith(".css")) {
                contentType = "text/css";
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

        for (String member : userDao.list()) {
            String firstName = member.split(",")[0];
            String lastName = member.split(",")[1];
            String email = member.split(",")[2];

            body += "<li>" + firstName + " " + lastName + " " + email + "</li>";
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

    public List<String> getMembers() throws SQLException {
        return userDao.list();
    }
}