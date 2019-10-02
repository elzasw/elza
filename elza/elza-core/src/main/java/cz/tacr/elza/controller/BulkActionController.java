package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.PersistentSortConfigVO;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.UserService;


/**
 * Controller pro hromadné akce
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
@RestController
@RequestMapping("/api/action")
public class BulkActionController {

    public static final String YAML_FILE_EXTENSION = ".yaml";
    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private UserService userService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDo;

    @RequestMapping(
            value = "/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public List<BulkActionVO> getBulkActions(final @PathVariable(value = "versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionList(bulkActionService.getBulkActions(fundVersionId));
    }

    @RequestMapping(value = "/list/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public List<BulkActionRunVO> getBulkActionsList(final @PathVariable("versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionsList(bulkActionService.getAllArrBulkActionRun(fundVersionId));
    }

    @RequestMapping(value = "/action/{bulkActionRunId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public BulkActionRunVO getBulkAction(final @PathVariable("bulkActionRunId") Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId, "Identifikátor běhu hromadné akce musí být vyplněn");
        return factoryVo.createBulkActionRunWithNodes(bulkActionService.getArrBulkActionRun(bulkActionRunId));
    }

    @RequestMapping(value = "/action/{bulkActionRunId}/interrupt",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public void interruptBulkAction(final @PathVariable("bulkActionRunId") Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId, "Identifikátor běhu hromadné akce musí být vyplněn");
        bulkActionService.interruptBulkAction(bulkActionRunId);
    }


    @RequestMapping(value = "/queue/{versionId}/{code}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public BulkActionRunVO queueByFa(final @PathVariable("versionId") Integer fundVersionId,
	        final @PathVariable("code") String code) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(code, "Kód musí být vyplněn");
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;
        return factoryVo.createBulkActionRun(bulkActionService.queue(userId, code, fundVersionId));
    }

    @RequestMapping(value = "/queue/{versionId}/{code}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public BulkActionRunVO queueByIds(final @PathVariable("versionId") Integer fundVersionId,
	        final @PathVariable("code") String code,
                    final @RequestBody List<Integer> nodeIds) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(code, "Kód musí být vyplněn");
        Assert.notEmpty(nodeIds, "Pro sputění hromadné akce je vyžadován alespoň 1 uzel");
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;

        ArrBulkActionRun actionRun = bulkActionService.queue(userId, code, fundVersionId, nodeIds, null);
        return factoryVo.createBulkActionRun(actionRun);
    }

    @RequestMapping(value = "/queue/persistentSort/{versionId}/{code}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public BulkActionRunVO queuePersistentSortByIds(final @PathVariable("versionId") Integer fundVersionId,
	        final @PathVariable("code") String code,
            final @RequestBody PersistentSortConfigVO configVO) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(code, "Kód musí být vyplněn");
        Assert.notNull(configVO, "Nastavení musí být vyplněno");
        Assert.notEmpty(configVO.getNodeIds(), "Pro sputění hromadné akce je vyžadován alespoň 1 uzel");

        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;

        PersistentSortRunConfig persistentSortRunConfig = factoryDo.createPersistentSortRunConfig(configVO);
        ArrBulkActionRun actionRun = bulkActionService.queue(userId, code, fundVersionId, configVO.getNodeIds(),
                persistentSortRunConfig);
        return factoryVo.createBulkActionRun(actionRun);
    }

    /**
     * Získání hromadných akcí outputu
     * @param outputId Id outputu
     * @param recommended doporučené / všechny
     *
     * @return list
     */
    @RequestMapping(value = "/output/{outputId}", method = RequestMethod.GET)
	@Transactional
    public List<BulkActionRunVO> getOutputBulkActions(@PathVariable final Integer outputId,
                                                 @RequestParam(required = false, defaultValue = "false") @Nullable final Boolean recommended) {
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        final ArrOutput output = outputService.getOutput(outputId);
        final Set<Integer> nodeIds = output.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .map(ArrNodeOutput::getNodeId)
                .collect(Collectors.toSet());
        final List<ArrBulkActionRun> bulkActionsByNodes = nodeIds.isEmpty() ? Collections.EMPTY_LIST :
                bulkActionService.findBulkActionsByNodeIds(
                        arrangementService.getOpenVersionByFundId(output.getFund().getFundId()),
                        nodeIds
                );
        final List<RulAction> recommendedActions = bulkActionService.getRecommendedActions(output.getOutputType());

        bulkActionsByNodes.sort((o1, o2) -> o2.getChange().getChangeId() - o1.getChange().getChangeId());
        ArrayList<BulkActionRunVO> result = new ArrayList<>();
        if (recommended != null && recommended) {
            for(final RulAction action : recommendedActions) {
                ArrBulkActionRun bulkActionRun = null;
                for (final ArrBulkActionRun run : bulkActionsByNodes) {
                    if (action.getFilename().equals(run.getBulkActionCode() + YAML_FILE_EXTENSION)) {
                        bulkActionRun = run;
                        break;
                    }
                }

                if (bulkActionRun != null) {
                    result.add(factoryVo.createBulkActionRun(bulkActionRun));
                } else {
                    BulkActionRunVO bulkActionRunVO = new BulkActionRunVO();
                    bulkActionRunVO.setCode(action.getFilename().replace(YAML_FILE_EXTENSION, ""));
                    result.add(bulkActionRunVO);
                }
            }
        } else {
            result.addAll(factoryVo.createBulkActionsList(bulkActionsByNodes));
            for(final RulAction action : recommendedActions) {
                ArrBulkActionRun bulkActionRun = null;
                for (final ArrBulkActionRun run : bulkActionsByNodes) {
                    if (action.getFilename().equals(run.getBulkActionCode() + YAML_FILE_EXTENSION)) {
                        bulkActionRun = run;
                        break;
                    }
                }

                if (bulkActionRun == null) {
                    BulkActionRunVO bulkActionRunVO = new BulkActionRunVO();
                    bulkActionRunVO.setCode(action.getFilename().replace(YAML_FILE_EXTENSION, ""));
                    result.add(bulkActionRunVO);
                }
            }
        }

        return result;
    }
}
