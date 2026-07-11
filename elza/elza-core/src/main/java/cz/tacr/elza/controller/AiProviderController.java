package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.aiprovider.client.vo.ProfileInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskParameterInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskTypeInfo;
import cz.tacr.elza.controller.vo.AiConversationCreateVO;
import cz.tacr.elza.controller.vo.AiConversationDetailVO;
import cz.tacr.elza.controller.vo.AiConversationVO;
import cz.tacr.elza.controller.vo.AiMyKeyUpdateVO;
import cz.tacr.elza.controller.vo.AiMyKeyVO;
import cz.tacr.elza.controller.vo.AiRequestCreateVO;
import cz.tacr.elza.controller.vo.AiRequestEventVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.controller.vo.AiProfileVO;
import cz.tacr.elza.controller.vo.AiProviderInfoVO;
import cz.tacr.elza.controller.vo.AiTaskParameterVO;
import cz.tacr.elza.controller.vo.AiTaskTypeVO;
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

    @Override
    @Transactional
    public ResponseEntity<AiProviderInfoVO> aiProviderGetInfo(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        return ResponseEntity.ok(toVO(aiProviderService.getInfo(extSystem)));
    }

    /** Maps the provider's /info document to the client's typed task catalog. */
    private AiProviderInfoVO toVO(final AiServiceInfo info) {
        AiProviderInfoVO vo = new AiProviderInfoVO()
                .protocolVersion(info.getProtocolVersion())
                .providerName(info.getProviderName());
        if (info.getTaskTypes() != null) {
            for (TaskTypeInfo taskType : info.getTaskTypes()) {
                vo.addTaskTypesItem(toVO(taskType));
            }
        }
        if (info.getProfiles() != null) {
            for (ProfileInfo profile : info.getProfiles()) {
                vo.addProfilesItem(new AiProfileVO()
                        .code(profile.getCode())
                        .name(profile.getName())
                        .description(profile.getDescription())
                        ._default(profile.getDefault()));
            }
        }
        return vo;
    }

    private AiTaskTypeVO toVO(final TaskTypeInfo taskType) {
        AiTaskTypeVO vo = new AiTaskTypeVO()
                .code(taskType.getCode())
                .name(taskType.getName())
                .description(taskType.getDescription());
        if (taskType.getResultTypes() != null) {
            taskType.getResultTypes().forEach(rt -> vo.addResultTypesItem(rt.getValue()));
        }
        if (taskType.getParameters() != null) {
            for (TaskParameterInfo param : taskType.getParameters()) {
                vo.addParametersItem(new AiTaskParameterVO()
                        .name(param.getName())
                        .type(param.getType() == null ? null : param.getType().getValue())
                        .description(param.getDescription())
                        .required(param.getRequired()));
            }
        }
        return vo;
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
