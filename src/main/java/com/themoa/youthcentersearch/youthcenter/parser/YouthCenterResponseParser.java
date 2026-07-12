package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiResponseException;
import com.themoa.youthcentersearch.youthcenter.client.ExternalApiResponse;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyDetail;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyList;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.SchemaAnalysis;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.YouthPolicyItem;
import com.themoa.youthcentersearch.youthcenter.dto.external.YouthCenterApiEnvelope;
import com.themoa.youthcentersearch.youthcenter.dto.external.YouthCenterApiResult;
import com.themoa.youthcentersearch.youthcenter.dto.external.YouthCenterPaging;
import com.themoa.youthcentersearch.youthcenter.dto.external.YouthCenterPolicyRaw;
import com.themoa.youthcentersearch.youthcenter.mapper.YouthPolicyMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class YouthCenterResponseParser {
    private final ObjectMapper objectMapper;
    private final ResponseTypeDetector typeDetector;
    private final ResponseSchemaAnalyzer schemaAnalyzer;
    private final YouthPolicyMapper mapper;

    public YouthCenterResponseParser(ObjectMapper objectMapper, ResponseTypeDetector typeDetector,
                                     ResponseSchemaAnalyzer schemaAnalyzer, YouthPolicyMapper mapper) {
        this.objectMapper = objectMapper;
        this.typeDetector = typeDetector;
        this.schemaAnalyzer = schemaAnalyzer;
        this.mapper = mapper;
    }

    public ResponseType detect(ExternalApiResponse response) {
        return typeDetector.detect(response.contentType(), response.body());
    }

    public ParsedPolicyList parseList(ExternalApiResponse response) {
        ResponseType type = detect(response);
        assertHttpAndBody(response, type);
        return switch (type) {
            case JSON -> parseJsonList(response);
            case XML -> parseXmlList(response);
            case EMPTY -> new ParsedPolicyList(false, null, List.of(), null, null, null,
                    emptySchema(), false, null, null);
            case HTML -> throw responseException("?뺤콉 JSON/XML ???HTML ?묐떟??諛쏆븯?듬땲?? ?몄쬆?? URL, Redirect ?먮뒗 ?쒕퉬???곹깭瑜??뺤씤?섏꽭??",
                    response, type, null, null);
            default -> throw responseException("?묐떟 ?뺤떇???뺤씤?????놁뒿?덈떎.", response, type, null, null);
        };
    }

    public ParsedPolicyDetail parseDetail(ExternalApiResponse response) {
        ResponseType type = detect(response);
        assertHttpAndBody(response, type);
        return switch (type) {
            case JSON -> parseJsonDetail(response);
            case XML -> parseXmlDetail(response);
            case HTML -> throw responseException("?뺤콉 JSON/XML ???HTML ?묐떟??諛쏆븯?듬땲?? ?몄쬆?? URL, Redirect ?먮뒗 ?쒕퉬???곹깭瑜??뺤씤?섏꽭??",
                    response, type, null, null);
            default -> throw responseException("?곸꽭 ?묐떟 ?뺤떇???뺤씤?????놁뒿?덈떎.", response, type, null, null);
        };
    }

    private ParsedPolicyList parseJsonList(ExternalApiResponse response) {
        JsonNode root = readJson(response);
        SchemaAnalysis analysis = schemaAnalyzer.analyzeJson(root);
        ApiError error = jsonError(root);
        if (error.isError()) {
            throw responseException("?⑦넻泥?뀈 API ?ㅻ쪟 ?묐떟?낅땲?? " + error.message(), response,
                    ResponseType.JSON, error.code(), error.message());
        }
        JsonNode array = currentPolicyList(root);
        if (array == null) {
            array = bestArray(root, analysis);
        }
        if (array == null) {
            YouthCenterPaging paging = readEnvelope(root).result() == null ? null : readEnvelope(root).result().effectivePaging();
            return new ParsedPolicyList(false, null, List.of(),
                    firstIntCandidate(root, "total", "totcnt", "totCount"),
                    firstIntCandidate(root, "pageNum", "currentPage", "page"),
                    firstIntCandidate(root, "pageSize", "display"), analysis, false, null, null);
        }
        List<YouthPolicyItem> policies = new ArrayList<>();
        array.forEach(item -> {
            if (item.isObject()) {
                policies.add(mapper.fromJson(item));
            }
        });
        YouthCenterApiEnvelope envelope = readEnvelope(root);
        YouthCenterPaging paging = envelope.result() == null ? null : envelope.result().effectivePaging();
        int arraySize = array.size();
        String path = currentPolicyList(root) != null ? "$.result.youthPolicyList" : analysis.candidateArrays().stream()
                .filter(candidate -> candidate.size() == arraySize)
                .findFirst()
                .map(SchemaAnalysis.CandidateArray::path)
                .orElse(null);
        return new ParsedPolicyList(true, path, policies,
                paging != null ? paging.totCount() : firstIntCandidate(root, "total", "totcnt", "totCount"),
                paging != null ? paging.pageNum() : firstIntCandidate(root, "pageNum", "currentPage", "page"),
                paging != null ? paging.pageSize() : firstIntCandidate(root, "pageSize", "display"),
                analysis, false, null, null);
    }

    private ParsedPolicyDetail parseJsonDetail(ExternalApiResponse response) {
        JsonNode root = readJson(response);
        SchemaAnalysis analysis = schemaAnalyzer.analyzeJson(root);
        ApiError error = jsonError(root);
        if (error.isError()) {
            throw responseException("?⑦넻泥?뀈 API ?ㅻ쪟 ?묐떟?낅땲?? " + error.message(), response,
                    ResponseType.JSON, error.code(), error.message());
        }
        DetailNode detail;
        try {
            detail = findCurrentPolicyDetail(root);
        } catch (IllegalStateException ex) {
            throw responseException(ex.getMessage(), response, ResponseType.JSON, null, null);
        }
        if (detail.node() == null) {
            throw responseException("?곸꽭 ?묐떟?먯꽌 $.result.youthPolicyList[0] ?뺤콉 ?곗씠?곕? 李얠? 紐삵뻽?듬땲??",
                    response, ResponseType.JSON, null, null);
        }
        return new ParsedPolicyDetail(mapper.fromJson(detail.node()), analysis, false, null, null, detail.warnings());
    }

    private ParsedPolicyList parseXmlList(ExternalApiResponse response) {
        Document document = readXml(response);
        SchemaAnalysis analysis = schemaAnalyzer.analyzeXml(document);
        ApiError error = xmlError(document);
        if (error.isError()) {
            throw responseException("?⑦넻泥?뀈 API ?ㅻ쪟 ?묐떟?낅땲?? " + error.message(), response,
                    ResponseType.XML, error.code(), error.message());
        }
        Element repeated = bestRepeatedElement(document);
        if (repeated == null) {
            return new ParsedPolicyList(false, null, List.of(), null, null, null, analysis, false, null, null);
        }
        List<YouthPolicyItem> policies = new ArrayList<>();
        NodeList siblings = repeated.getParentNode().getChildNodes();
        for (int i = 0; i < siblings.getLength(); i++) {
            if (siblings.item(i) instanceof Element element && element.getTagName().equals(repeated.getTagName())) {
                policies.add(mapper.fromMap(elementToMap(element)));
            }
        }
        return new ParsedPolicyList(true, "/" + repeated.getParentNode().getNodeName() + "/" + repeated.getTagName(),
                policies, null, null, null, analysis, false, null, null);
    }

    private ParsedPolicyDetail parseXmlDetail(ExternalApiResponse response) {
        Document document = readXml(response);
        SchemaAnalysis analysis = schemaAnalyzer.analyzeXml(document);
        ApiError error = xmlError(document);
        if (error.isError()) {
            throw responseException("?⑦넻泥?뀈 API ?ㅻ쪟 ?묐떟?낅땲?? " + error.message(), response,
                    ResponseType.XML, error.code(), error.message());
        }
        Element repeated = bestRepeatedElement(document);
        Element item = repeated == null ? document.getDocumentElement() : repeated;
        return new ParsedPolicyDetail(mapper.fromMap(elementToMap(item)), analysis, false, null, null);
    }

    private JsonNode currentPolicyList(JsonNode root) {
        JsonNode list = root.path("result").path("youthPolicyList");
        return list.isArray() ? list : null;
    }

    private DetailNode findCurrentPolicyDetail(JsonNode root) {
        List<String> warnings = new ArrayList<>();
        JsonNode list = root.path("result").path("youthPolicyList");
        if (list.isArray()) {
            if (list.isEmpty()) {
                throw new IllegalStateException("?곸꽭 ?묐떟??$.result.youthPolicyList媛 鍮?諛곗뿴?낅땲??");
            }
            if (list.size() > 1) {
                warnings.add("?곸꽭 ?묐떟???뺤콉???щ윭 嫄??ы븿?섏뼱 泥?踰덉㎏ ?뺤콉???ъ슜?덉뒿?덈떎.");
            }
            if (list.get(0).isObject()) {
                return new DetailNode(list.get(0), warnings);
            }
        }
        JsonNode policy = root.path("result").path("youthPolicy");
        if (policy.isObject()) {
            return new DetailNode(policy, warnings);
        }
        JsonNode dataDetail = root.path("data").path("detail");
        if (dataDetail.isObject()) {
            warnings.add("援ъ“ fallback?쇰줈 $.data.detail???ъ슜?덉뒿?덈떎.");
            return new DetailNode(dataDetail, warnings);
        }
        JsonNode fallback = firstPolicyObject(root);
        if (fallback != null) {
            warnings.add("?쇰컲 ?ㅽ궎留??먯깋 fallback?쇰줈 ?뺤콉 媛앹껜瑜??좏깮?덉뒿?덈떎.");
        }
        return new DetailNode(fallback, warnings);
    }

    private YouthCenterApiEnvelope readEnvelope(JsonNode root) {
        try {
            YouthCenterApiEnvelope envelope = objectMapper.treeToValue(root, YouthCenterApiEnvelope.class);
            return envelope == null ? new YouthCenterApiEnvelope(null, null, null) : envelope;
        } catch (JsonProcessingException ex) {
            return new YouthCenterApiEnvelope(null, null, null);
        }
    }

    private void assertHttpAndBody(ExternalApiResponse response, ResponseType type) {
        if (response.statusCode() >= 300 && response.statusCode() < 400) {
            throw responseException("?⑦넻泥?뀈 API媛 HTTP " + response.statusCode() + " Redirect瑜?諛섑솚?덉뒿?덈떎. Location ?ㅻ뜑瑜??뺤씤?섏꽭??",
                    response, type, null, null);
        }
        if (response.statusCode() >= 400) {
            throw responseException("?⑦넻泥?뀈 API媛 HTTP " + response.statusCode() + " ?ㅻ쪟瑜?諛섑솚?덉뒿?덈떎.",
                    response, type, null, null);
        }
    }

    private JsonNode readJson(ExternalApiResponse response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException ex) {
            throw responseException("JSON ?뚯떛 ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.", response, ResponseType.JSON,
                    null, ex.getOriginalMessage());
        }
    }

    private Document readXml(ExternalApiResponse response) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.body())));
        } catch (Exception ex) {
            throw responseException("XML ?뚯떛 ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.", response, ResponseType.XML, null, ex.getMessage());
        }
    }

    private JsonNode bestArray(JsonNode root, SchemaAnalysis analysis) {
        SchemaAnalysis.CandidateArray best = analysis.candidateArrays().stream()
                .filter(candidate -> candidate.fields().stream().anyMatch(this::looksLikePolicyField))
                .findFirst()
                .orElse(analysis.candidateArrays().stream().findFirst().orElse(null));
        return best == null ? null : nodeAtPath(root, best.path());
    }

    private boolean looksLikePolicyField(String field) {
        String lower = field.toLowerCase(Locale.ROOT);
        return lower.contains("plcy") || lower.contains("policy");
    }

    private JsonNode firstPolicyObject(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isObject() && hasPolicyField(root)) {
            return root;
        }
        if (root.isArray() && !root.isEmpty() && root.get(0).isObject()) {
            return root.get(0);
        }
        if (root.isObject()) {
            Iterator<JsonNode> values = root.elements();
            while (values.hasNext()) {
                JsonNode found = firstPolicyObject(values.next());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean hasPolicyField(JsonNode node) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            if (looksLikePolicyField(names.next())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode nodeAtPath(JsonNode root, String path) {
        JsonNode current = root;
        String cleaned = path.replace("$", "");
        if (cleaned.isBlank()) {
            return current;
        }
        for (String part : cleaned.split("\\.")) {
            if (part.isBlank()) {
                continue;
            }
            String name = part.replaceAll("\\[\\d+\\]", "");
            if (!name.isBlank()) {
                current = current.get(name);
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private ApiError jsonError(JsonNode root) {
        String resultCode = textAtAnyDepth(root, "resultCode");
        String resultMsg = textAtAnyDepth(root, "resultMsg");
        if (resultMsg == null) {
            resultMsg = textAtAnyDepth(root, "resultMessage");
        }
        String error = textAtAnyDepth(root, "error");
        String code = textAtAnyDepth(root, "code");
        String message = textAtAnyDepth(root, "message");
        if (notSuccessCode(resultCode)) {
            return new ApiError(true, resultCode, resultMsg);
        }
        if ((error != null && !error.isBlank()) || (code != null && message != null && !message.isBlank())) {
            return new ApiError(true, code == null ? error : code, message == null ? resultMsg : message);
        }
        return new ApiError(false, null, null);
    }

    private ApiError xmlError(Document document) {
        String code = firstElementText(document, "resultCode");
        String msg = firstElementText(document, "resultMsg");
        if (notSuccessCode(code)) {
            return new ApiError(true, code, msg);
        }
        return new ApiError(false, null, null);
    }

    private boolean notSuccessCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim();
        return !"00".equals(normalized)
                && !"0".equals(normalized)
                && !"200".equals(normalized)
                && !"SUCCESS".equalsIgnoreCase(normalized)
                && !"OK".equalsIgnoreCase(normalized);
    }

    private String textAtAnyDepth(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        if (node.isObject() && node.has(fieldName)) {
            JsonNode value = node.get(fieldName);
            return value == null || value.isNull() ? null : value.asText();
        }
        if (node.isContainerNode()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                String found = textAtAnyDepth(iterator.next(), fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Integer intCandidate(JsonNode root, String hint) {
        String value = findTextByHint(root, hint.toLowerCase(Locale.ROOT));
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer firstIntCandidate(JsonNode root, String... hints) {
        for (String hint : hints) {
            Integer candidate = intCandidate(root, hint);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String findTextByHint(JsonNode node, String hint) {
        if (node == null || !node.isContainerNode()) {
            return null;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getKey().toLowerCase(Locale.ROOT).contains(hint) && entry.getValue().isValueNode()) {
                    return entry.getValue().asText();
                }
                String nested = findTextByHint(entry.getValue(), hint);
                if (nested != null) {
                    return nested;
                }
            }
        } else {
            for (JsonNode child : node) {
                String nested = findTextByHint(child, hint);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Element bestRepeatedElement(Document document) {
        NodeList all = document.getElementsByTagName("*");
        Map<String, Integer> counts = new LinkedHashMap<>();
        Element first = null;
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            counts.merge(element.getTagName(), 1, Integer::sum);
            if (first == null && elementToMap(element).keySet().stream().anyMatch(this::looksLikePolicyField)) {
                first = element;
            }
        }
        if (first != null) {
            return first;
        }
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (counts.getOrDefault(element.getTagName(), 0) > 1) {
                return element;
            }
        }
        return null;
    }

    private Map<String, Object> elementToMap(Element element) {
        Map<String, Object> values = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child) {
                values.put(child.getTagName(), child.getTextContent());
            }
        }
        return values;
    }

    private String firstElementText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private SchemaAnalysis emptySchema() {
        return new SchemaAnalysis(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, List.of(), List.of());
    }

    private YouthCenterApiResponseException responseException(String message, ExternalApiResponse response,
                                                              ResponseType type, String code, String apiMessage) {
        return new YouthCenterApiResponseException(message, response.statusCode(), code, apiMessage,
                response.contentType(), type, response.maskedRequestUrl(), preview(response.body()), response.redirectLocation());
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 1000 ? body : body.substring(0, 1000);
    }

    private record ApiError(boolean isError, String code, String message) {
    }

    private record DetailNode(JsonNode node, List<String> warnings) {
    }
}
