package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // Spring Data Jpa

    List<Member> findByUsername(String username); //메서드 명으로 자동 생성, 간단한 정적쿼리 구현
}
