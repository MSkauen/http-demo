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
        new Thread(() -> {
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
        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println(requestLine);

        String requestMethod = requestLine.split(" ")[0];
        String requestTarget = requestLine.split(" ")[1];

        String fullName = "";
        String emailAddress = "";

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            QueryString requestParameters = new QueryString(request.getBody());
            fullName = requestParameters.getParameter("full_name");
            emailAddress = requestParameters.getParameter("email_address");

            if (fullName != null || emailAddress != null) {
                String requestParametersDecoded = java.net.URLDecoder.decode("\r\n" + fullName + "\r\n" + emailAddress + "\r\n", StandardCharsets.UTF_8);
                members.add(requestParametersDecoded);
            }

            String body = "Ok";
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "\r\n" +
                    body;

            clientSocket.getOutputStream().write(response.getBytes());
            clientSocket.close();
            return;
        } else {
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else {
                    File file = new File(contentRoot, requestPath);
                    if (!file.exists()) {
                        String body = file + " does not exist";
                        String response = "HTTP/1.1 404 Not found\r\n" +
                                "Content-Length: " + body.length() + "\r\n" +
                                "\r\n" +
                                body;

                        clientSocket.getOutputStream().write(response.getBytes());
                        clientSocket.close();
                        return;
                    }

                    if (requestPath.equals("/")) {
                        file = new File(contentRoot, "/index.html");
                    }

                    String statusCode = "200";
                    String contentType = "text/plain";
                    if (file.getName().endsWith(".html")) {
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
                    clientSocket.close();
                    return;
                }
        }

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
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
        clientSocket.close();
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
