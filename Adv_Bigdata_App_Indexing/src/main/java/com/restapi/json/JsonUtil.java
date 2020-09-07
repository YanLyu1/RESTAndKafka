package com.restapi.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JsonUtil {

	private final static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

	private static JsonSchema jsonSchema = null;
	private final static JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

	public static final String ID = "id_";

	public static void loadSchema() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

		// Read the json schema
		File initialFile = new File("./schema.json");

		try {
			InputStream schema = new FileInputStream(initialFile);
			JsonNode schemaNode = mapper.readTree(schema);
			// Add the input.json as the schema to validate all inputs against
			jsonSchema = factory.getJsonSchema(schemaNode);
			logger.info("get json schema from: " + initialFile.getPath());
		} catch (Exception e) {
			logger.error("Error loading the json schema");
		}
	}

	/**
	 * Added to validate json against the given schema
	 * 
	 * @param inputJson
	 * @return
	 */
	public static JsonNode validateJSON(String inputJson) {
		JsonNode output = null;

		try {
			JsonNode inputNode = getJsonNode(inputJson);
			ProcessingReport processingReport = jsonSchema.validate(inputNode);

			if (processingReport.isSuccess()) {
				return inputNode;
			}
		} catch (Exception e) {

			logger.error(e.getMessage());
		}
		return output;
	}

	public static JsonNode getRootNode(JsonNode rootNode) {
		traverse(rootNode, 1);
		return rootNode;
	}

	private static void traverse(JsonNode node, int level) {
		if (node.getNodeType() == JsonNodeType.ARRAY) {
			traverseArray(node, level);
		} else if (node.getNodeType() == JsonNodeType.OBJECT) {
			traverseObject(node, level);
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	private static void traverseObject(JsonNode node, int level) {
		node.fieldNames().forEachRemaining((String fieldName) -> {
			JsonNode childNode = node.get(fieldName);
			printNode(childNode, fieldName, level);
			// for nested object or arrays
			if (traversable(childNode)) {
				traverse(childNode, level + 1);
			}
		});
	}

	private static void traverseArray(JsonNode node, int level) {
		for (JsonNode jsonArrayNode : node) {
			printNode(jsonArrayNode, "arrayElement", level);
			if (traversable(jsonArrayNode)) {
				traverse(jsonArrayNode, level + 1);
			}
		}
	}

	private static boolean traversable(JsonNode node) {
		return node.getNodeType() == JsonNodeType.OBJECT || node.getNodeType() == JsonNodeType.ARRAY;
	}

	private static void printNode(JsonNode node, String keyName, int level) {
		if (traversable(node)) {
			System.out.printf("%" + (level * 4 - 3) + "s|-- %s=%s type=%s%n", "", keyName, node.toString(),
					node.getNodeType());

		} else {
			Object value = null;
			if (node.isTextual()) {
				value = node.textValue();
			} else if (node.isNumber()) {
				value = node.numberValue();
			} // todo add more types
			System.out.printf("%" + (level * 4 - 3) + "s|-- %s=%s type=%s%n", "", keyName, value, node.getNodeType());
		}
	}

	public static JsonNode getJsonNode(String string) throws IOException {

		if (string == null) {
			return null;
		}else if (string.isEmpty()) {
			string = "{}";
		}else {
			return JsonLoader.fromString(string);
		}
		return null;
	}

}
