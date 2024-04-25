package cz.tacr.elza.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class ExceptionResponse {

    private String type;

    private String code;

    private String message;

    private String level;

    private Map<String, Object> properties;

    private String stackTrace;

    public ExceptionResponse() {
    }

	public ExceptionResponse(String type, String code, String message, String level, Map<String, Object> properties, String stackTrace) {
		this.type = type;
		this.code = code;
		this.message = message;
		this.level = level;
		this.properties = properties;
		this.stackTrace = stackTrace;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public void setStackTrace(String stackTrace) {
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
