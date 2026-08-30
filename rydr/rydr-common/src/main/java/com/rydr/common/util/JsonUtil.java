package com.rydr.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON utility based on Jackson.
 * Replaces the unmaintained net.sf.json-lib (json-lib) that was used throughout the project.
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    /**
     * @return the shared ObjectMapper instance
     */
    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Creates a new JSON object node (replacement for new JSONObject()).
     */
    public static ObjectNode newObject() {
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * Serializes an object to a JSON string, falling back to toString() on failure.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    /**
     * Parses a JSON string into a tree, returning an empty object node on failure.
     */
    public static JsonNode parse(String json) {
        if (json == null || json.isEmpty()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    /**
     * Converts any JSON-compatible value (Map, JsonNode, POJO) into the given type.
     */
    public static <T> T toBean(Object fromValue, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(fromValue, clazz);
    }

    /**
     * Converts a JSON string into the given type.
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
