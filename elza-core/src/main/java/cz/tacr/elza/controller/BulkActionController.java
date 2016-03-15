package cz.tacr.elza.controller;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping(value = "/run/{versionId}/{code}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    BulkActionStateVO run(final @PathVariable("versionId") Integer fundVersionId, final @PathVariable("code") String code) {
        BulkActionConfig action = bulkActionService.getBulkAction(code);
        return factoryVo.createBulkActionState(bulkActionService.run(action, fundVersionId));
    }

    @RequestMapping(value = "/states/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionStateVO> getBulkActionState(final @PathVariable("versionId") Integer fundVersionId) {
        return factoryVo.createBulkActionStateList(bulkActionService.getBulkActionState(fundVersionId));
    }
}
