package cz.tacr.elza.controller;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * Controller pro hromadné akce
 *
 * @author Petr Comple [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
@RestController
@RequestMapping("/api/bulkActionManagerV2")
public class BulkActionController {

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @RequestMapping(value = "/{versionId}/{mandatory}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionVO> getBulkActions(Integer findingAidVersionId, Boolean mandatory) {
        return factoryVo.createBulkActionList(bulkActionService.getBulkActions(findingAidVersionId, mandatory));
    }

    @RequestMapping(value = "/validate/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionVO> validate(Integer findingAidVersionId) {
        return factoryVo.createBulkActionList(bulkActionService.runValidation(findingAidVersionId));
    }

    @RequestMapping(value = "/run/{versionId}/{code}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    BulkActionStateVO run(Integer findingAidVersionId, String code) {
        BulkActionConfig action = bulkActionService.getBulkAction(code);
        return factoryVo.createBulkActionState(bulkActionService.run(action, findingAidVersionId));
    }

    @RequestMapping(value = "/states/{versionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<BulkActionStateVO> getBulkActionState(Integer findingAidVersionId) {
        return factoryVo.createBulkActionStateList(bulkActionService.getBulkActionState(findingAidVersionId));
    }
}
