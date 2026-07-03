package cz.tacr.elza.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.service.AiProviderService;

/**
 * Endpoints for AI providers (external systems speaking the
 * "Elza AI Provider API"). Implements the generated {@link AiproviderApi}.
 */
@RestController
@RequestMapping("/api/v1")
public class AiProviderController implements AiproviderApi {

    @Autowired
    private AiProviderService aiProviderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> aiProviderGetInfo(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        AiServiceInfo info = aiProviderService.getInfo(extSystem);
        Map<String, Object> body = objectMapper.convertValue(info, new TypeReference<Map<String, Object>>() {
        });
        return ResponseEntity.ok(body);
    }
}
