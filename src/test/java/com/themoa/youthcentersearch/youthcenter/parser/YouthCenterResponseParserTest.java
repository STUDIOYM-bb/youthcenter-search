package com.themoa.youthcentersearch.youthcenter.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.youthcenter.client.ExternalApiResponse;
import com.themoa.youthcentersearch.youthcenter.mapper.YouthPolicyMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YouthCenterResponseParserTest {
    private final YouthCenterResponseParser parser = new YouthCenterResponseParser(
            new ObjectMapper(),
            new ResponseTypeDetector(),
            new ResponseSchemaAnalyzer(),
            new YouthPolicyMapper()
    );

    @Test
    void parsesCurrentListStructure() {
        String body = """
                {
                  "resultCode": 200,
                  "resultMessage": "성공적으로 데이터를 가지고 왔습니다.",
                  "result": {
                    "pagging": {"totCount": 3, "pageNum": 1, "pageSize": 2},
                    "youthPolicyList": [
                      {"plcyNo": "P001", "plcyNm": "청년 취업 지원", "plcyKywdNm": "청년,취업", "plcyExplnCn": "취업 준비 지원"}
                    ]
                  }
                }
                """;

        var parsed = parser.parseList(response(body));

        assertThat(parsed.listNodePath()).isEqualTo("$.result.youthPolicyList");
        assertThat(parsed.totalCount()).isEqualTo(3);
        assertThat(parsed.currentPage()).isEqualTo(1);
        assertThat(parsed.pageSize()).isEqualTo(2);
        assertThat(parsed.policies()).hasSize(1);
        assertThat(parsed.policies().get(0).policyNumber()).isEqualTo("P001");
    }

    @Test
    void parsesCurrentDetailFromYouthPolicyListFirstItem() {
        String body = """
                {
                  "resultCode": 200,
                  "result": {
                    "youthPolicyList": [
                      {"plcyNo": "P001", "plcyNm": "청년 취업 지원", "plcyExplnCn": "취업 준비 지원"}
                    ]
                  }
                }
                """;

        var parsed = parser.parseDetail(response(body));

        assertThat(parsed.policy().policyNumber()).isEqualTo("P001");
        assertThat(parsed.policy().policyName()).isEqualTo("청년 취업 지원");
    }

    private ExternalApiResponse response(String body) {
        return new ExternalApiResponse(200, "OK", "application/json", Map.of(), body,
                "https://www.youthcenter.go.kr/go/ythip/getPlcy?apiKeyNm=****", null,
                List.of(), "https://www.youthcenter.go.kr/go/ythip/getPlcy?apiKeyNm=****", List.of(), 10);
    }
}
