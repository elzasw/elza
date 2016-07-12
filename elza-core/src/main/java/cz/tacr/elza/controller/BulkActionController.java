package cz.tacr.elza.controller;

import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Controller pro hromadné akce
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
@RestController
@RequestMapping("/api/bulkActionManagerV2")
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

    @RequestMapping(
            value = "/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionVO> getBulkActions(final @PathVariable(value = "versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionList(bulkActionService.getBulkActions(fundVersionId));
    }

    @RequestMapping(value = "/list/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionRunVO> getBulkActionsList(final @PathVariable("versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionsList(bulkActionService.getAllArrBulkActionRun(fundVersionId));
    }

    @RequestMapping(value = "/action/{bulkActionRunId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    BulkActionRunVO getBulkAction(final @PathVariable("bulkActionRunId") Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId);
        return factoryVo.createBulkActionRunWithNodes(bulkActionService.getArrBulkActionRun(bulkActionRunId));
    }

    @RequestMapping(value = "/action/{bulkActionRunId}/interrupt",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void interruptBulkAction(final @PathVariable("bulkActionRunId") Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId);
        bulkActionService.interruptBulkAction(bulkActionRunId);
    }


    @RequestMapping(value = "/queue/{versionId}/{code}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    BulkActionRunVO queueByFa(final @PathVariable("versionId") Integer fundVersionId, final @PathVariable("code") String code) {
        Assert.notNull(fundVersionId);
        Assert.notNull(code);
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;
        return factoryVo.createBulkActionRun(bulkActionService.queue(userId, code, fundVersionId));
    }

    @RequestMapping(value = "/queue/{versionId}/{code}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    BulkActionRunVO queueByIds(final @PathVariable("versionId") Integer fundVersionId, final @PathVariable("code") String code,
                    final @RequestBody List<Integer> nodeIds) {
        Assert.notNull(fundVersionId);
        Assert.notNull(code);
        Assert.notEmpty(nodeIds, "Pro sputění hromadné akce je vyžadován alespon 1 uzel");
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;
        return factoryVo.createBulkActionRun(bulkActionService.queue(userId, code, fundVersionId, nodeIds));
    }

    /**
     * Získání hromadných akcí outputu
     * @param outputId Id outputu
     * @param recommended doporučené / všechny
     *
     * @return list
     */
    @RequestMapping(value = "/output/{outputId}", method = RequestMethod.GET)
    public List<BulkActionRunVO> findOutputFiles(@PathVariable final Integer outputId,
                                                 @RequestParam(required = false, defaultValue = "false") @Nullable final Boolean recommended) {
        Assert.notNull(outputId);
        final ArrOutput output = outputService.getOutput(outputId);
        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();
        final Set<ArrNode> nodes = outputDefinition.getOutputNodes().stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet());
        final List<ArrBulkActionRun> bulkActionsByNodes = nodes.isEmpty() ? Collections.EMPTY_LIST :
                bulkActionService.findBulkActionsByNodes(
                        arrangementService.getOpenVersionByFundId(outputDefinition.getFund().getFundId()),
                        nodes,
                        State.FINISHED, State.RUNNING, State.OUTDATED, State.ERROR, State.INTERRUPTED, State.OUTDATED
                );
        final Set<RulAction> recommendedActions = bulkActionService.getRecommendedActions(outputDefinition.getOutputType());

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
