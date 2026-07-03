package cz.tacr.elza.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.controller.vo.AiConversationCreateVO;
import cz.tacr.elza.controller.vo.AiConversationDetailVO;
import cz.tacr.elza.controller.vo.AiConversationVO;
import cz.tacr.elza.controller.vo.AiMyKeyUpdateVO;
import cz.tacr.elza.controller.vo.AiMyKeyVO;
import cz.tacr.elza.controller.vo.AiRequestCreateVO;
import cz.tacr.elza.controller.vo.AiRequestEventVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.service.AiProviderService;
import cz.tacr.elza.service.ai.AiConversationService;

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
    private AiConversationService aiConversationService;

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

    @Override
    public ResponseEntity<AiConversationDetailVO> aiProviderCreateConversation(
            AiConversationCreateVO aiConversationCreateVO) {
        return ResponseEntity.ok(aiConversationService.createConversation(aiConversationCreateVO));
    }

    @Override
    public ResponseEntity<AiConversationDetailVO> aiProviderGetConversation(Integer id) {
        return ResponseEntity.ok(aiConversationService.getConversation(id));
    }

    @Override
    public ResponseEntity<List<AiConversationVO>> aiProviderListConversations(String contextType) {
        return ResponseEntity.ok(aiConversationService.listConversations(contextType));
    }

    @Override
    public ResponseEntity<AiConversationDetailVO> aiProviderCreateRequest(Integer id,
            AiRequestCreateVO aiRequestCreateVO) {
        return ResponseEntity.ok(aiConversationService.createRequest(id, aiRequestCreateVO));
    }

    @Override
    public ResponseEntity<AiRequestVO> aiProviderCancelRequest(Integer id) {
        return ResponseEntity.ok(aiConversationService.cancelRequest(id));
    }

    @Override
    public ResponseEntity<List<AiRequestEventVO>> aiProviderListRequestEvents(Integer id) {
        return ResponseEntity.ok(aiConversationService.listRequestEvents(id));
    }

    @Override
    public ResponseEntity<AiMyKeyVO> aiProviderGetMyKey(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        return ResponseEntity.ok(aiProviderService.getMyKey(extSystem));
    }

    @Override
    public ResponseEntity<AiMyKeyVO> aiProviderSetMyKey(String id, AiMyKeyUpdateVO aiMyKeyUpdateVO) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        return ResponseEntity.ok(aiProviderService.setMyKey(extSystem, aiMyKeyUpdateVO));
    }

    @Override
    public ResponseEntity<Void> aiProviderDeleteMyKey(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        aiProviderService.deleteMyKey(extSystem);
        return ResponseEntity.noContent().build();
    }
}
