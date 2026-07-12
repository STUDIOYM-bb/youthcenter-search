package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.youthcenter.dto.parsed.SchemaAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ResponseSchemaAnalyzer {
    public SchemaAnalysis analyzeJson(JsonNode root) {
        List<String> rootFields = new ArrayList<>();
        List<String> objectPaths = new ArrayList<>();
        List<SchemaAnalysis.CandidateArray> arrays = new ArrayList<>();
        List<String> numberCandidates = new ArrayList<>();
        List<String> nameCandidates = new ArrayList<>();
        List<String> totalCandidates = new ArrayList<>();
        List<String> pageCandidates = new ArrayList<>();
        if (root != null && root.isObject()) {
            root.fieldNames().forEachRemaining(rootFields::add);
        }
        walkJson(root, "$", objectPaths, arrays, numberCandidates, nameCandidates, totalCandidates, pageCandidates);
        return new SchemaAnalysis(rootFields, objectPaths, arrays, numberCandidates, nameCandidates,
                totalCandidates, pageCandidates, null, List.of(), List.of());
    }

    public SchemaAnalysis analyzeXml(Document document) {
        Element root = document.getDocumentElement();
        List<String> children = childElementNames(root);
        Map<String, Integer> counts = new HashMap<>();
        collectElementCounts(root, "/" + root.getTagName(), counts);
        List<SchemaAnalysis.XmlRepeatedElement> repeated = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new SchemaAnalysis.XmlRepeatedElement(entry.getKey(), entry.getValue(), List.of()))
                .toList();
        return new SchemaAnalysis(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                root.getTagName(), children, repeated);
    }

    private void walkJson(JsonNode node, String path, List<String> objectPaths,
                          List<SchemaAnalysis.CandidateArray> arrays, List<String> numberCandidates,
                          List<String> nameCandidates, List<String> totalCandidates, List<String> pageCandidates) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            objectPaths.add(path);
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String childPath = path + "." + entry.getKey();
                String lower = entry.getKey().toLowerCase();
                if (lower.contains("plcyno") || lower.contains("policyno")) numberCandidates.add(childPath);
                if (lower.contains("plcynm") || lower.contains("policyname") || lower.equals("title")) nameCandidates.add(childPath);
                if (lower.contains("total") || lower.contains("totcnt")) totalCandidates.add(childPath);
                if (lower.contains("pagenum") || lower.contains("pageindex") || lower.contains("currentpage")) pageCandidates.add(childPath);
                walkJson(entry.getValue(), childPath, objectPaths, arrays, numberCandidates, nameCandidates, totalCandidates, pageCandidates);
            }
            return;
        }
        if (node.isArray()) {
            List<String> fields = new ArrayList<>();
            if (!node.isEmpty() && node.get(0).isObject()) {
                node.get(0).fieldNames().forEachRemaining(fields::add);
            }
            arrays.add(new SchemaAnalysis.CandidateArray(path, node.size(), fields));
            for (int i = 0; i < node.size(); i++) {
                walkJson(node.get(i), path + "[" + i + "]", objectPaths, arrays, numberCandidates, nameCandidates, totalCandidates, pageCandidates);
            }
        }
    }

    private List<String> childElementNames(Element element) {
        Set<String> names = new LinkedHashSet<>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element child) {
                names.add(child.getTagName());
            }
        }
        return new ArrayList<>(names);
    }

    private void collectElementCounts(Element element, String path, Map<String, Integer> counts) {
        counts.merge(path, 1, Integer::sum);
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element child) {
                collectElementCounts(child, path + "/" + child.getTagName(), counts);
            }
        }
    }
}
