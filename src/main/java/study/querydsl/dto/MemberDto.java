package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor //기본생성자
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection //dto Qtype 생성
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

}
