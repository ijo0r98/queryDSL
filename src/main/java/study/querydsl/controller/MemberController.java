package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    // 조회 API 컨트롤러 개발
    /**
     * test와 local에서 웹서버를 돌릴 때 구분하여 테스트 케이스 주입
     * test: test용 데이터만
     * local: init()
     */

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/member")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhereParam(condition);
    }

}
