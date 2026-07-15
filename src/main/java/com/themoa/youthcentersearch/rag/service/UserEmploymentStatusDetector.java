package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatusResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 사용자 원문에서 현재 취업 상태를 판정한다.
 * 취업 정책을 원하는지 여부는 PolicyIntentPolarityDetector/검색 의미 분석이 맡고, 이 클래스는 현재 상태만 다룬다.
 */
@Component
public class UserEmploymentStatusDetector {
    private static final Pattern EMPLOYED = Pattern.compile(
            "직장[에을]?\\s*다니|회사[에를]?\\s*다니|회사에서\\s*일하|직장에서\\s*일하|현재\\s*일하고|현재\\s*근무\\s*중|근무하고|재직\\s*중|재직하고|재직자|직장인|회사원|근로자"
    );
    private static final Pattern UNEMPLOYED = Pattern.compile(
            "현재\\s*무직|무직|미취업|직장이\\s*없|회사[를에]?\\s*다니지\\s*않|현재\\s*쉬고|퇴사\\s*후.{0,12}취업하지|구직\\s*중|취업\\s*준비\\s*중|취준생|직장[을를]?\\s*(?:구하|찾고)|취업하지\\s*않"
    );
    private static final Pattern FUTURE_OR_WISH = Pattern.compile(
            "직장[에을]?\\s*다니고\\s*싶|회사에\\s*들어갈\\s*예정|취업하면.{0,12}직장[에을]?\\s*다닐\\s*예정"
    );

    public UserEmploymentStatusResult detect(String query) {
        if (query == null || query.isBlank()) {
            return UserEmploymentStatusResult.unknown();
        }
        String text = query.replaceAll("\\s+", " ");
        if (FUTURE_OR_WISH.matcher(text).find()) {
            return UserEmploymentStatusResult.unknown();
        }
        var employed = EMPLOYED.matcher(text);
        if (employed.find()) {
            return new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 0.95,
                    List.of("RULE_EXPLICIT: " + employed.group()));
        }
        var unemployed = UNEMPLOYED.matcher(text);
        if (unemployed.find()) {
            return new UserEmploymentStatusResult(UserEmploymentStatus.UNEMPLOYED, true, 0.95,
                    List.of("RULE_EXPLICIT: " + unemployed.group()));
        }
        return UserEmploymentStatusResult.unknown();
    }
}
