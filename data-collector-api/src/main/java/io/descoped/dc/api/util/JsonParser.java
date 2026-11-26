package io.descoped.dc.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;

/**
 * A Jackson object mapper for JSON or YAML, with helpers that throw unchecked exceptions.
 */
public interface JsonParser {

    /**
     * The object mapper.
     *
     * @return the object mapper
     */
    ObjectMapper mapper();

    /**
     * A new empty object node.
     *
     * @return the node
     */
    ObjectNode createObjectNode();

    /**
     * A new empty array node.
     *
     * @return the node
     */
    ArrayNode createArrayNode();

    /**
     * Reads a value from a stream of UTF-8 text.
     *
     * @param source the stream
     * @param clazz  the value's class
     * @param <T>    the value's type
     * @return the value
     * @throws RuntimeException if the source cannot be read
     */
    <T> T fromJson(InputStream source, Class<T> clazz);

    /**
     * Reads a value from text.
     *
     * @param source the text
     * @param clazz  the value's class
     * @param <T>    the value's type
     * @return the value
     * @throws RuntimeException if the source cannot be read
     */
    <T> T fromJson(String source, Class<T> clazz);

    /**
     * Writes a value as text.
     *
     * @param value the value
     * @return the text
     * @throws RuntimeException if the value cannot be written
     */
    String toJSON(Object value);

    /**
     * Writes a value as indented text.
     *
     * @param value the value
     * @return the text
     * @throws RuntimeException if the value cannot be written
     */
    String toPrettyJSON(Object value);

    /**
     * A JSON parser on a shared object mapper.
     *
     * @return the parser
     */
    static JsonParser createJsonParser() {
        return new JsonParserImpl();
    }

    /**
     * A parser on an object mapper.
     *
     * @param objectMapper the object mapper
     * @return the parser
     */
    static JsonParser createJsonParser(ObjectMapper objectMapper) {
        return new JsonParserImpl(objectMapper);
    }

    /**
     * A YAML parser on a shared object mapper.
     *
     * @return the parser
     */
    static JsonParser createYamlParser() {
        return new YamlParserImpl();
    }

    /**
     * A YAML parser on a new object mapper of a YAML factory.
     *
     * @param yamlFactory the YAML factory
     * @return the parser
     */
    static JsonParser createYamlParser(YAMLFactory yamlFactory) {
        return new YamlParserImpl(yamlFactory);
    }

    /**
     * A parser on an object mapper, used as given.
     *
     * @param objectMapper the object mapper
     * @return the parser
     */
    static JsonParser createYamlParser(ObjectMapper objectMapper) {
        return new YamlParserImpl(objectMapper);
    }

}
