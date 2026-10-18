package consulo.openapi.internal;

import consulo.endpoint.oas.OasArrayValue;
import consulo.endpoint.oas.OasComponents;
import consulo.endpoint.oas.OasEndpointPath;
import consulo.endpoint.oas.OasExample;
import consulo.endpoint.oas.OasExampleValue;
import consulo.endpoint.oas.OasHeader;
import consulo.endpoint.oas.OasMediaTypeObject;
import consulo.endpoint.oas.OasNullValue;
import consulo.endpoint.oas.OasObjectValue;
import consulo.endpoint.oas.OasOperation;
import consulo.endpoint.oas.OasParameter;
import consulo.endpoint.oas.OasParameterIn;
import consulo.endpoint.oas.OasParameterStyle;
import consulo.endpoint.oas.OasPrimitiveTypeValue;
import consulo.endpoint.oas.OasProperty;
import consulo.endpoint.oas.OasRequestBody;
import consulo.endpoint.oas.OasResponse;
import consulo.endpoint.oas.OasSchema;
import consulo.endpoint.oas.OasSchemaFormat;
import consulo.endpoint.oas.OasSchemaType;
import consulo.endpoint.oas.OasTag;
import consulo.endpoint.oas.OpenApiSpecification;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class OpenApiDocumentTree {
    public static final String OPENAPI_VERSION = "3.0.3";

    private static final String DEFAULT_RESPONSE_CODE = "200";
    private static final String DEFAULT_RESPONSE_DESCRIPTION = "OK";

    private OpenApiDocumentTree() {
    }

    public static Map<String, Object> build(OpenApiSpecification specification, String title, String version) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", OPENAPI_VERSION);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", title);
        info.put("version", version);
        root.put("info", info);

        List<OasTag> tags = specification.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<Object> tagList = new ArrayList<>();
            for (OasTag tag : tags) {
                Map<String, Object> tagMap = new LinkedHashMap<>();
                tagMap.put("name", tag.getName());
                putIfNotBlank(tagMap, "description", tag.getDescription());
                tagList.add(tagMap);
            }
            root.put("tags", tagList);
        }

        Map<String, Object> paths = new LinkedHashMap<>();
        for (OasEndpointPath path : specification.getPaths()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(path.getAbsolutePath(), key -> new LinkedHashMap<String, Object>());
            for (OasOperation operation : path.getOperations()) {
                pathItem.put(operation.getMethod().getMethodName(), operation(operation));
            }
        }
        root.put("paths", paths);

        OasComponents components = specification.getComponents();
        if (components != null && !components.getSchemas().isEmpty()) {
            Map<String, Object> schemas = new LinkedHashMap<>();
            for (Map.Entry<String, OasSchema> entry : components.getSchemas().entrySet()) {
                schemas.put(entry.getKey(), schema(entry.getValue()));
            }
            Map<String, Object> componentMap = new LinkedHashMap<>();
            componentMap.put("schemas", schemas);
            root.put("components", componentMap);
        }
        return root;
    }

    private static Map<String, Object> operation(OasOperation operation) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!operation.getTags().isEmpty()) {
            result.put("tags", new ArrayList<Object>(operation.getTags()));
        }
        putIfNotBlank(result, "summary", operation.getSummary());
        putIfNotBlank(result, "description", operation.getDescription());
        putIfNotBlank(result, "operationId", operation.getOperationId());
        if (operation.isDeprecated()) {
            result.put("deprecated", true);
        }

        List<Object> parameters = new ArrayList<>();
        for (OasParameter parameter : operation.getParameters()) {
            parameters.add(parameter(parameter));
        }
        if (!parameters.isEmpty()) {
            result.put("parameters", parameters);
        }

        OasRequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            result.put("requestBody", requestBody(requestBody));
        }

        Map<String, Object> responses = new LinkedHashMap<>();
        for (OasResponse response : operation.getResponses()) {
            responses.put(response.getCode(), response(response));
        }
        if (responses.isEmpty()) {
            Map<String, Object> defaultResponse = new LinkedHashMap<>();
            defaultResponse.put("description", DEFAULT_RESPONSE_DESCRIPTION);
            responses.put(DEFAULT_RESPONSE_CODE, defaultResponse);
        }
        result.put("responses", responses);
        return result;
    }

    private static Map<String, Object> parameter(OasParameter parameter) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", parameter.getName());
        result.put("in", parameter.getInPlace().getPlaceName());
        putIfNotBlank(result, "description", parameter.getDescription());
        if (parameter.isRequired() || parameter.getInPlace() == OasParameterIn.PATH) {
            result.put("required", true);
        }
        if (parameter.isDeprecated()) {
            result.put("deprecated", true);
        }
        OasParameterStyle style = parameter.getStyle();
        if (style != null && style != OasParameterStyle.DEFAULT) {
            result.put("style", style.getValue());
        }
        OasSchema schema = parameter.getSchema();
        result.put("schema", schema != null ? schema(schema) : stringSchema());
        return result;
    }

    private static Map<String, Object> requestBody(OasRequestBody requestBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (requestBody.getRequired()) {
            result.put("required", true);
        }
        Map<String, Object> content = new LinkedHashMap<>();
        for (Map.Entry<String, OasSchema> entry : requestBody.getContent().entrySet()) {
            Map<String, Object> mediaType = new LinkedHashMap<>();
            mediaType.put("schema", schema(entry.getValue()));
            content.put(entry.getKey(), mediaType);
        }
        result.put("content", content);
        return result;
    }

    private static Map<String, Object> response(OasResponse response) {
        Map<String, Object> result = new LinkedHashMap<>();
        String description = response.getDescription();
        result.put("description", description == null || description.isBlank() ? DEFAULT_RESPONSE_DESCRIPTION : description);

        if (!response.getHeaders().isEmpty()) {
            Map<String, Object> headers = new LinkedHashMap<>();
            for (OasHeader header : response.getHeaders()) {
                Map<String, Object> headerMap = new LinkedHashMap<>();
                if (header.getRequired()) {
                    headerMap.put("required", true);
                }
                headerMap.put("schema", schema(header.getSchema()));
                headers.put(header.getName(), headerMap);
            }
            result.put("headers", headers);
        }

        if (!response.getContent().isEmpty()) {
            Map<String, Object> content = new LinkedHashMap<>();
            for (Map.Entry<String, OasMediaTypeObject> entry : response.getContent().entrySet()) {
                content.put(entry.getKey(), mediaType(entry.getValue()));
            }
            result.put("content", content);
        }
        return result;
    }

    private static Map<String, Object> mediaType(OasMediaTypeObject mediaType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", schema(mediaType.getSchema()));
        if (!mediaType.getExamples().isEmpty()) {
            Map<String, Object> examples = new LinkedHashMap<>();
            for (Map.Entry<String, OasExample> entry : mediaType.getExamples().entrySet()) {
                Map<String, Object> example = new LinkedHashMap<>();
                putIfNotBlank(example, "summary", entry.getValue().getSummary());
                putIfNotBlank(example, "description", entry.getValue().getDescription());
                example.put("value", exampleValue(entry.getValue().getValue()));
                examples.put(entry.getKey(), example);
            }
            result.put("examples", examples);
        }
        return result;
    }

    private static Map<String, Object> schema(OasSchema schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        String reference = schema.getReference();
        if (reference != null) {
            result.put("$ref", reference);
            return result;
        }
        OasSchemaType type = schema.getType();
        if (type != null) {
            result.put("type", type.name().toLowerCase(Locale.ROOT));
        }
        OasSchemaFormat format = schema.getFormat();
        if (format != null) {
            result.put("format", format.getFormatName());
        }
        List<String> enumValues = schema.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            result.put("enum", new ArrayList<Object>(enumValues));
        }
        putIfNotBlank(result, "default", schema.getDefault());
        if (schema.isNullable()) {
            result.put("nullable", true);
        }
        OasSchema items = schema.getItems();
        if (items != null) {
            result.put("items", schema(items));
        }
        if (schema.getAreItemsUnique()) {
            result.put("uniqueItems", true);
        }
        List<OasProperty> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> propertyMap = new LinkedHashMap<>();
            for (OasProperty property : properties) {
                propertyMap.put(property.getName(), schema(property.getSchema()));
            }
            result.put("properties", propertyMap);
        }
        List<String> required = schema.getRequired();
        if (required != null && !required.isEmpty()) {
            result.put("required", new ArrayList<Object>(required));
        }
        return result;
    }

    private static Map<String, Object> stringSchema() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "string");
        return result;
    }

    private static @Nullable Object exampleValue(OasExampleValue value) {
        if (value instanceof OasNullValue) {
            return null;
        }
        if (value instanceof OasPrimitiveTypeValue<?> primitive) {
            return primitive.getValue();
        }
        if (value instanceof OasArrayValue array) {
            List<Object> result = new ArrayList<>();
            for (OasExampleValue item : array.getItems()) {
                result.add(exampleValue(item));
            }
            return result;
        }
        OasObjectValue object = (OasObjectValue) value;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, OasExampleValue> entry : object.getProperties().entrySet()) {
            result.put(entry.getKey(), exampleValue(entry.getValue()));
        }
        return result;
    }

    private static void putIfNotBlank(Map<String, Object> map, String key, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
