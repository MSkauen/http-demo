package no.kristiania.http;

import no.kristiania.database.Member;
import no.kristiania.database.MemberDao;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class MemberPostController implements HttpController {

    private MemberDao memberDao;

    public MemberPostController(MemberDao memberDao) {

        this.memberDao = memberDao;
    }

    @Override
    public void handle(HttpMessage request, Socket clientSocket) throws IOException, SQLException {
        QueryString requestParameters = new QueryString(request.getBody());

        String firstName = requestParameters.getParameter("first_name");
        String lastName = requestParameters.getParameter("last_name");
        String emailAddress = requestParameters.getParameter("email_address");

        Member member = new Member();
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmailAddress(emailAddress);

        memberDao.insert(member);

        String body = "Ok";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }
}
