package cz.tacr.elza.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class ExceptionResponse {

    private final String type;

    private final String code;

    private final String message;

    private final String level;

    private final Map<String, Object> properties;

    private final String stackTrace;

    public ExceptionResponse(String type,
                             String code,
                             String message,
                             String level,
                             Map<String, Object> properties,
                             String stackTrace) {
        this.type = type;
        this.code = code;
        this.message = message;
        this.level = level;
        this.properties = properties;
        this.stackTrace = stackTrace;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getLevel() {
        return level;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    @JsonIgnore
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // exception during exception ... hope not
            throw new RuntimeException(e);
        }
    }
}
