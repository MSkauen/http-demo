package no.kristiania.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


class MemberDaoTest {

    private MemberDao memberDao;
    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();
        memberDao = new MemberDao(dataSource);
    }

    @Test
    void shouldListInsertedMembers() throws SQLException {
        Member member1 = exampleMember();
        Member member2 = exampleMember();
        memberDao.insert(member1);
        memberDao.insert(member2);
        assertThat(memberDao.list())
                .extracting(member -> member.getFirstName())
                .contains(member1.getFirstName(), member2.getFirstName());
    }

    @Test
    void shouldRetrieveAllMemberProperties() throws SQLException {
        memberDao.insert(exampleMember());
        memberDao.insert(exampleMember());
        Member member = exampleMember();
        memberDao.insert(member);
        assertThat(member).hasNoNullFieldsOrProperties();
        assertThat(memberDao.retrieve(member.getId()))
                .usingRecursiveComparison()
                .isEqualTo(member);
    }

    private Member exampleMember() {
        Member member = new Member();
        member.setFirstName(exampleMemberFirstName());
        member.setLastName(exampleMemberLastName());
        member.setEmailAddress(exampleMemberEmailAddress());
        return member;
    }

    private String exampleMemberFirstName() {
        String[] firstNameOptions = {"Ola", "Hans", "Petter", "Silje"};
        return firstNameOptions[random.nextInt(firstNameOptions.length)];
    }
    private String exampleMemberLastName() {
        String[] lastNameOptions = {"Nordmann", "Fjell", "Stein", "Skogen"};
        return lastNameOptions[random.nextInt(lastNameOptions.length)];
    }
    private String exampleMemberEmailAddress() {
        String[] emailOptions = {"minepost@test.no", "enepost@mail.com", "epost1@test.no", "email@epost.no"};
        return emailOptions[random.nextInt(emailOptions.length)];
    }
}