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
    private HttpServer server;
    private MemberDao memberDao;

    @BeforeEach
    void setUp() throws IOException {
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).load().migrate();
        server = new HttpServer(0, dataSource);
        memberDao = new MemberDao(dataSource);
    }

    @Test
    void shouldReturnSuccessfulStatusCode() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo");
        assertEquals(200, client.getStatusCode());
    }

    @Test
    void shouldReturnUnsuccessfulStatusCode() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?status=404");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldReturnContentLength() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("10", client.getResponseHeader("Content-Length"));
    }

    @Test
    void shouldReturnResponseBody() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("HelloWorld", client.getResponseBody());
    }

    @Test
    void shouldReturnFileFromDisk() throws IOException {
        File contentRoot = new File("target/test-classes");

        String fileContent = "Hello World " + new Date();
        Files.writeString(new File(contentRoot, "test.txt").toPath(), fileContent);

        HttpClient client = new HttpClient("localhost", server.getPort(), "/test.txt");
        assertEquals(fileContent, client.getResponseBody());
        assertEquals("text/plain", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturnCorrectContentType() throws IOException {
        File contentRoot = new File("target/test-classes");

        Files.writeString(new File(contentRoot, "index.html").toPath(), "<h2>Hello World</h2>");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/index.html");
        assertEquals("text/html", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturn404IfFileNotFound() throws IOException {
        File contentRoot = new File("target/test-classes");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/notFound.txt");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldPostNewMember() throws IOException, SQLException {
        String requestBody = "first_name=test&last_name=bruker&email_address=test@email.no";
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/members/newMember", "POST", requestBody);
        assertEquals(200, client.getStatusCode());
        assertThat(memberDao.list())
                .extracting(Member::getFirstName)
                .contains("test");
    }

    @Test
    void shouldReturnExistingMembers() throws IOException, SQLException {
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
        String requestBody = "project_name=test&project_color=#5a3434&project_status=To-Do";
        HttpClient postClient = new HttpClient("localhost", server.getPort(), "/api/projects/newProject", "POST", requestBody);
        assertEquals(200, postClient.getStatusCode());

        HttpClient getClient = new HttpClient("localhost", server.getPort(), "/api/projects/listProjects");
        assertThat(getClient.getResponseBody()).contains("<li>test #5a3434 To-Do</li>");
    }
}
