package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import cz.tacr.elza.aiprovider.client.vo.AccountUsageInfo;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.aiprovider.client.vo.CustomerUsageInfo;
import cz.tacr.elza.aiprovider.client.vo.ProfileInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskParameterInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskTypeInfo;
import cz.tacr.elza.aiprovider.client.vo.UsageInfo;
import cz.tacr.elza.controller.vo.AiAccountUsageVO;
import cz.tacr.elza.controller.vo.AiConversationCreateVO;
import cz.tacr.elza.controller.vo.AiConversationDetailVO;
import cz.tacr.elza.controller.vo.AiConversationVO;
import cz.tacr.elza.controller.vo.AiCustomerUsageVO;
import cz.tacr.elza.controller.vo.AiProposalDecisionRequestVO;
import cz.tacr.elza.controller.vo.AiRequestCreateVO;
import cz.tacr.elza.controller.vo.AiRequestEventVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.controller.vo.AiProfileVO;
import cz.tacr.elza.controller.vo.AiProviderInfoVO;
import cz.tacr.elza.controller.vo.AiTaskParameterVO;
import cz.tacr.elza.controller.vo.AiTaskTypeVO;
import cz.tacr.elza.controller.vo.AiUsageBalanceVO;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.service.AiProviderService;
import cz.tacr.elza.service.ai.AiConversationService;
import cz.tacr.elza.service.ai.AiProposalService;

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
    private AiProposalService aiProposalService;

    @Override
    @Transactional
    public ResponseEntity<AiProviderInfoVO> aiProviderGetInfo(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        return ResponseEntity.ok(toVO(aiProviderService.getInfo(extSystem)));
    }

    @Override
    @Transactional
    public ResponseEntity<AiUsageBalanceVO> aiProviderGetUsage(String id) {
        AiExternalSystem extSystem = aiProviderService.findAiSystemByCodeOrId(id);
        return ResponseEntity.ok(toVO(aiProviderService.getUsage(extSystem)));
    }

    /** Maps the provider's /usage document to the client's balance view. */
    private AiUsageBalanceVO toVO(final UsageInfo usage) {
        AiUsageBalanceVO vo = new AiUsageBalanceVO();
        AccountUsageInfo account = usage.getAccount();
        if (account != null) {
            vo.setAccount(new AiAccountUsageVO()
                    .accountType(account.getAccountType())
                    .plan(account.getPlan())
                    .planName(account.getPlanName())
                    .allowanceCredits(account.getAllowanceCredits())
                    .spentCredits(account.getSpentCredits())
                    .periodStart(account.getPeriodStart())
                    .periodEnd(account.getPeriodEnd())
                    .weeklyAllowanceCredits(account.getWeeklyAllowanceCredits())
                    .weeklySpentCredits(account.getWeeklySpentCredits())
                    .weekStart(account.getWeekStart())
                    .weekEnd(account.getWeekEnd()));
        }
        CustomerUsageInfo customer = usage.getCustomer();
        if (customer != null) {
            vo.setCustomer(new AiCustomerUsageVO()
                    .budgetCredits(customer.getBudgetCredits())
                    .spentCredits(customer.getSpentCredits())
                    .periodStart(customer.getPeriodStart()));
        }
        return vo;
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
    public ResponseEntity<AiRequestVO> aiProviderApplyProposalChange(Integer id,
            AiProposalDecisionRequestVO aiProposalDecisionRequestVO) {
        return ResponseEntity.ok(aiProposalService.applyChange(id, aiProposalDecisionRequestVO.getChangeKey()));
    }

    @Override
    public ResponseEntity<AiRequestVO> aiProviderRejectProposalChange(Integer id,
            AiProposalDecisionRequestVO aiProposalDecisionRequestVO) {
        return ResponseEntity.ok(aiProposalService.rejectChange(id, aiProposalDecisionRequestVO.getChangeKey()));
    }

    @Override
    public ResponseEntity<List<AiRequestEventVO>> aiProviderListRequestEvents(Integer id) {
        return ResponseEntity.ok(aiConversationService.listRequestEvents(id));
    }
}
