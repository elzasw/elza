package cz.tacr.elza.controller;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Controller pro hromadné akce
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
@RestController
@RequestMapping("/api/bulkActionManagerV2")
public class BulkActionController {

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @RequestMapping(
            value = "/{versionId}/{mandatory}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionVO> getBulkActions(final @PathVariable(value = "versionId") Integer fundVersionId,
                                      final @PathVariable(value = "mandatory") Boolean mandatory) {
        return factoryVo.createBulkActionList(bulkActionService.getBulkActions(fundVersionId, mandatory));
    }

    @RequestMapping(value = "/validate/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionVO> validate(final @PathVariable("versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionList(bulkActionService.runValidation(fundVersionId));
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
        Assert.notEmpty(nodeIds);
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail != null ? userDetail.getId() : null;
        return factoryVo.createBulkActionRun(bulkActionService.queue(userId, code, fundVersionId, nodeIds));
    }
}
