package no.kristiania.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDao extends AbstractDao<Member> {

    public MemberDao(DataSource dataSource) {
        super(dataSource);
    }

    public void insert(Member member) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO members (first_name, last_name, email_address) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, member.getFirstName());
                statement.setString(2, member.getLastName());
                statement.setString(3, member.getEmailAddress());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    generatedKeys.next();
                    member.setId(generatedKeys.getLong("id"));
                }
            }
        }
    }

    public Member retrieve(Long id) throws SQLException {
        return retrieve(id, "SELECT * FROM members WHERE id = ?");
    }

    public List<Member> list() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM members")) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<Member> members = new ArrayList<>();
                    while (rs.next()) {
                        members.add(mapRow(rs));
                    }
                    return members;
                }
            }
        }
    }

    @Override
    protected Member mapRow(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setId(rs.getLong("id"));
        member.setFirstName(rs.getString("first_name"));
        member.setLastName(rs.getString("last_name"));
        member.setEmailAddress(rs.getString("email_address"));
        return member;
    }
}