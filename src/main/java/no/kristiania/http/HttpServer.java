package no.kristiania.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpServer {

    private File contentRoot;
    private List<String> members = new ArrayList<>();

    public HttpServer(int port) throws IOException {

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server running on port: " + port + "\r\n Access server using any IP-Address:" + port + ", e.g 127.0.0.1:" + port + " or localhost:" + port);
        new Thread (() -> {
            while (true) {
                try {

                    Socket clientSocket = serverSocket.accept();
                    handleRequest(clientSocket);
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void handleRequest(Socket clientSocket) throws IOException {
        String requestLine = HttpMessage.readLine(clientSocket);
        System.out.println(requestLine);

        String requestTarget = requestLine.split(" ")[1];
        String requestMethod = requestLine.split(" ")[0];
        System.out.println("INDEX 0: " + requestMethod);
        System.out.println("INDEX 1: " + requestTarget);
        String statusCode = "200";
        String body = "Hello World";

        String fullName = "";
        String emailAddress = "";

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (questionPos != -1) {
            QueryString queryString = new QueryString(requestTarget.substring(questionPos + 1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
            if (requestPath.equals("/members")) {
                if (queryString.getParameter("full_name") != null) {
                    fullName = queryString.getParameter("full_name");
                }
                if (queryString.getParameter("email_address") != null) {
                    emailAddress = queryString.getParameter("email_address");
                }
                String fileContent = java.net.URLDecoder.decode(fullName + "\r\n" + emailAddress + "\r\n" + "\r\n", StandardCharsets.UTF_8);
                members.add(fileContent);

            }
        }
        if (requestPath.equals("/members")) {
            body = members.toString();
            String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    body;
            clientSocket.getOutputStream().write(response.getBytes());
            return;
        }
        if (!requestPath.equals("/echo")) {
            File file = new File(contentRoot, requestPath);
            if (!file.exists()){
                body = file + " does not exist";
                String response = "HTTP/1.1 404 Not found\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;

                clientSocket.getOutputStream().write(response.getBytes());
                return;
            }

            if (requestPath.equals("/")) {
                file = new File(contentRoot, "/index.html");
            }

            statusCode = "200";
            String contentType = "text/plain";
            if (file.getName().endsWith(".html")){
                contentType = "text/html";
            } else if (file.getName().endsWith(".css")) {
                contentType = "text/css";
            }
            String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";

            // Write the response back to the client
            clientSocket.getOutputStream().write(response.getBytes());
            new FileInputStream(file).transferTo(clientSocket.getOutputStream());
        }

        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServer(8080);
        server.setContentRoot(new File("src/main/resources"));
    }

    public void setContentRoot(File contentRoot) {
        this.contentRoot = contentRoot;
    }

    public List<String> getMembers() {
        return members;
    }
}
