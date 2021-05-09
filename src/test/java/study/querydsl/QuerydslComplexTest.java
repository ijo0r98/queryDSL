package study.querydsl;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.h2.engine.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import com.querydsl.core.Tuple;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslComplexTest {
    // 중급 문법

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    // 튜플조회
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s: result) {
            System.out.println("s: " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        // 권장하지 않음, 레퍼지토리 안에서만 필요할때 사용하도록 -나갈때는 DTO로 변환할 것

        for (Tuple tuple: result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username: " + username);
            System.out.println("age: " + age);
        }
    }

    // 프로젝션과 DTO로 결과 반환

    @Test
    public void findDtoByJPQL() {
//        em.createQuery("select m.username, m.age from Member m", MemberDto.class); 안됨
        //생성자 방식만 지원
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto dto: result) {
            System.out.println("meberDto: " + dto);
        }
    }

    @Test
    public void findDtoBySetter() {
        // 기본 생성자가 있어야함 기본생성자 -> 값 주입, getter setter 있어야함
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto dto: result) {
            System.out.println("memberDto: " + dto);
        }
    }

    @Test
    public void findDtoByField() {
        // getter, setter 가 없어도 됨, 바로 필드에 값이 주입됨
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto dto: result) {
            System.out.println("memberDto: " + dto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        // 생성자 타입을 맞아야 함
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto dto: result) {
            System.out.println("memberDto: " + dto);
        }
    }

    @Test
    public void findUserDtoByField() {
        // username(Member) -> name(UserDto) 매칭이 안되기 때문에 무시됨 --> as("별칭")
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        // 서브쿼리 쓸 때
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result2 = queryFactory
                .select(Projections.fields(UserDto.class,
                        ExpressionUtils.as(member.username, "name"), //ExpressionUtils.as(source, alias)
                        ExpressionUtils.as(JPAExpressions //최대 나이로 나이 통일
                            .select(memberSub.age.max())
                                .from(memberSub), "age") //별칭
                        ))
                .from(member)
                .fetch();

        for (UserDto dto: result) {
            System.out.println("userDto: " + dto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        // 기본 생성자 있어야함
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto dto: result) {
            System.out.println("userDto: " + dto);
        }
    }


}
