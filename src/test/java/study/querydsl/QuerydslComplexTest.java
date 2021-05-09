package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.h2.engine.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import com.querydsl.core.Tuple;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
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
                        // member.id - runtime error
                        member.age))
                .from(member)
                .fetch();

        for (UserDto dto: result) {
            System.out.println("userDto: " + dto);
        }
    }

    // @QueryProjection성
    // 어노테이션이 붙은 dto도 qtype 생성

    @Test
    public void findUserDtoByQueryProjection() {
        // 기본 생성자 있어야함
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto dto: result) {
            System.out.println("userDto: " + dto);
        }
    }
    // ByConstructor - 컴파일 오류가 아닌 런타임 오류가 발생
    // QueryProjection - 컴파일 오류, 인텔리제이 자동완성
    /** 단점
     * DTO Qtype 생성
     * queryDSL에 의존성을 갖게 됨
     * 여러 레이어(서비스, 레퍼지토리, api반환)에 겨처 사용됨 - queryDSL에 의존적임으로 순수한 dto가 아님
     * 아키텍쳐적으로 깔끔하지 않음
     */


    // 동적쿼리
    // 1. BooleanBuilder

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder(); //초기 조건 인자로 설정 가능
//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); // username 필수
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond)); //조건이 맞으면 and query
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //2. where 다중 파라미터 사용

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) //null 일때 무시
//                .where(allEq(usernameCond, ageCond)) //조립
                .fetch();
    }

    // 메서드로 깔끔하게 분리 가능 *이름은 직관적으로!
    private BooleanExpression usernameEq(String usernameCond) {
//        if (usernameCond == null) {
//            return null;
//        }
//        return member.username.eq(usernameCond);

        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 검색조건 조립 가능, 조립할때는 BooleanExpression 반환값
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // 예로들어 광고 상태가 isValid(서비스 가능상태) + 날짜가 In .. -> isServiceable 조립 가능

    // 수정, 삭제, 배치 쿼리 벌크 연산 - 쿼리 한번으로 대량 데이터 수정

    @Test
//    @Commit //커밋해서 결과 확인 가능, 원래대로라면 rollback
    public void buildupdate() {
        // 영속성 컨텍스트
        // member1 10 -> member1
        // member2 20 -> member2
        // member3 30 -> member3
        // member4 40 -> member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();
        /**
         * 벌크 연산 후 초기화 해야함
         */

        // DataBase
        // member1 10 -> 비회원
        // member2 20 -> 비회원
        // member3 30 -> 유지
        // member4 40 -> 유지

        // 그 다음 db조회 시 영속성 컨텍스트에 다시 올려줘야함
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch(); //영속성 컨텍스트가 우선권을 가져 영속성 컨텍스트에 올려있는 결과를 반환
    }
    /**
     * member1~4 영속성 컨텍스트에 올라가있음
     * 벌크연산을 통해 쿼리가 실행되어 DB와 영속성 컨텍스트 사이 차이가 생김
     */

    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //빼기는 없음으로 -1 multiply(곱하기)
                .execute();
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    // SQL function 호출하기

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s: result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s: result) {
            System.out.println("s = " + s);
        }
    }




}
