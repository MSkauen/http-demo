package no.kristiania.http;

import no.kristiania.database.Member;
import no.kristiania.database.MemberDao;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpServerTest {

    private final JdbcDataSource dataSource = new JdbcDataSource();

    @BeforeEach
    void setUp() {
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();
    }

    @Test
    void shouldReturnSuccessfulStatusCode() throws IOException {
        HttpServer server = new HttpServer(10001, dataSource);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo");
        assertEquals(200, client.getStatusCode());
    }

    @Test
    void shouldReturnUnsuccessfulStatusCode() throws IOException {
        HttpServer server = new HttpServer(10002, dataSource);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?status=404");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldReturnContentLength() throws IOException {
        HttpServer server = new HttpServer(10003, dataSource);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("10", client.getResponseHeader("Content-Length"));
    }

    @Test
    void shouldReturnResponseBody() throws IOException {
        HttpServer server = new HttpServer(10004, dataSource);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("HelloWorld", client.getResponseBody());
    }

    @Test
    void shouldReturnFileFromDisk() throws IOException {
        HttpServer server = new HttpServer(10005, dataSource);
        File contentRoot = new File("target/test-classes");

        String fileContent = "Hello World " + new Date();
        Files.writeString(new File(contentRoot, "test.txt").toPath(), fileContent);

        HttpClient client = new HttpClient("localhost", server.getPort(), "/test.txt");
        assertEquals(fileContent, client.getResponseBody());
        assertEquals("text/plain", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturnCorrectContentType() throws IOException {
        HttpServer server = new HttpServer(10006, dataSource);
        File contentRoot = new File("target/test-classes");

        Files.writeString(new File(contentRoot, "index.html").toPath(), "<h2>Hello World</h2>");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/index.html");
        assertEquals("text/html", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturn404IfFileNotFound() throws IOException {
        HttpServer server = new HttpServer(10007, dataSource);
        File contentRoot = new File("target/test-classes");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/notFound.txt");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldPostNewMember() throws IOException, SQLException {
        HttpServer server = new HttpServer(10008, dataSource);
        String requestBody = "first_name=test&last_name=bruker&email_address=test@email.no";
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/members/newMember", "POST", requestBody);
        assertEquals(200, client.getStatusCode());
        assertThat(server.getMembers())
                .filteredOn(member -> member.getFirstName().equals("test"))
                .isNotEmpty()
                .satisfies(p -> assertThat(p.get(0).getLastName()).isEqualTo("bruker"));
    }

    @Test
    void shouldReturnExistingMembers() throws IOException, SQLException {
        HttpServer server = new HttpServer(10009, dataSource);
        MemberDao memberDao = new MemberDao(dataSource);
        Member member = new Member();
        member.setFirstName("Ola");
        member.setLastName("Nordmann");
        member.setEmailAddress("test@email.no");
        memberDao.insert(member);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/members/listMembers");
        assertThat(client.getResponseBody()).contains("<li>Ola Nordmann test@email.no</li>");
    }
    @Test
    void shouldPostNewProject() throws IOException {
        HttpServer server = new HttpServer(10010, dataSource);
        String requestBody = "project_name=test&project_color=#5a3434";
        HttpClient postClient = new HttpClient("localhost", server.getPort(), "/api/projects/newProject", "POST", requestBody);
        assertEquals(200, postClient.getStatusCode());

        HttpClient getClient = new HttpClient("localhost", server.getPort(), "/api/projects/listProjects");
        assertThat(getClient.getResponseBody()).contains("<li>test #5a3434</li>");
    }
}