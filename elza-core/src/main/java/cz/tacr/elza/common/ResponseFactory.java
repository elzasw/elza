package cz.tacr.elza.common;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.BaseException;
import cz.tacr.elza.controller.vo.ExportRequestState;
import cz.tacr.elza.controller.vo.ExportRequestStatus;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;
import cz.tacr.elza.exception.codes.PackageCode;

public class ResponseFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String jsonAsString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SystemException("Problem with JSON generation", e, BaseCode.JSON_PARSE);
        }
    }

    public static ExportRequestStatus createExportRequestStatus(ExportRequestState state) {
        ExportRequestStatus ers = new ExportRequestStatus();
        ers.setState(state);
        return ers;
    }

    public static BaseException createBaseException(Exception exception) {
        BaseException baseException = new BaseException();
        baseException.setMessage(exception.getMessage());
        baseException.setStackTrace(ExceptionUtils.getStackTrace(exception));

        Level level = null;
        ErrorCode errorCode = null;
        if (exception instanceof AbstractException) {
            AbstractException abException = (AbstractException) exception;
            level = abException.getLevel();
            errorCode = abException.getErrorCode();
            // check if it is Elze specific exception
            baseException.setProperties(abException.getProperties());
        }
        if (level == null) {
            level = Level.DANGER;
        }
        if (errorCode == null) {
            errorCode = BaseCode.EXPORT_FAILED;
        }
        baseException.setLevel(level.getLevel());

        baseException.setType(errorCode.getType());
        baseException.setCode(errorCode.getCode());
        return baseException;
    }

    public static ResponseEntity<Resource> responseException(int httpStateCode, Exception exception) {
        BaseException baseException = createBaseException(exception);

        String jsonString = ResponseFactory.jsonAsString(baseException);
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity
                .status(httpStateCode)
                .body(new InputStreamResource(new ByteArrayInputStream(bytes)));
    }

    public static ResponseEntity<Resource> responseFile(Path filePath, HttpHeaders headers) {
        if (!Files.exists(filePath)) {
            throw new SystemException("File not found: " + filePath, PackageCode.FILE_NOT_FOUND);
        }
        FileSystemResource fsr = new FileSystemResource(filePath);
        return new ResponseEntity<>(fsr, headers, HttpStatus.OK);
    }
}