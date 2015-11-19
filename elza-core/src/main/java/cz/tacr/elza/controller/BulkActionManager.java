package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.BulkActionState;


/**
 * Implementace API pro obsluhu hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@RestController
@RequestMapping("/api/bulkActionManager")
public class BulkActionManager
        implements cz.tacr.elza.api.controller.BulkActionManager<BulkActionConfig, BulkActionState> {

    @Autowired
    private BulkActionService bulkActionService;

    @Override
    @RequestMapping(value = "/bulkactiontypes", method = RequestMethod.GET)
    public List<String> getBulkActionTypes() {
        return bulkActionService.getBulkActionTypes();
    }

    @Override
    @RequestMapping(value = "/bulkaction", method = RequestMethod.PUT)
    public BulkActionConfig createBulkAction(@RequestBody final BulkActionConfig bulkActionConfig) {
        Assert.notNull(bulkActionConfig);
        return bulkActionService.createBulkAction(bulkActionConfig);
    }

    @Override
    @RequestMapping(value = "/bulkaction", method = RequestMethod.POST)
    public BulkActionConfig updateBulkAction(@RequestBody final BulkActionConfig bulkActionConfig) {
        Assert.notNull(bulkActionConfig);
        return bulkActionService.update(bulkActionConfig);
    }

    @Override
    @RequestMapping(value = "/bulkaction/{bulkActionCode}", method = RequestMethod.GET)
    public BulkActionConfig getBulkAction(@PathVariable(value = "bulkActionCode") final String bulkActionCode) {
        Assert.notNull(bulkActionCode);
        return bulkActionService.getBulkAction(bulkActionCode);
    }

    @Override
    @RequestMapping(value = "/bulkaction/{versionId}/states", method = RequestMethod.GET)
    public List<BulkActionState> getBulkActionState(@PathVariable(value = "versionId") final Integer findingAidVersionId) {
        Assert.notNull(findingAidVersionId);
        return bulkActionService.getBulkActionState(findingAidVersionId);
    }

    @Override
    @RequestMapping(value = "/bulkaction", method = RequestMethod.DELETE)
    public void deleteBulkAction(@RequestBody final BulkActionConfig bulkActionConfig) {
        Assert.notNull(bulkActionConfig);
        bulkActionService.delete(bulkActionConfig);
    }

    @Override
    @RequestMapping(value = "/reload", method = RequestMethod.GET)
    public void reload() {
        bulkActionService.reload();
    }

    @Override
    @RequestMapping(value = "/bulkactions/{versionId}", method = RequestMethod.GET)
    public List<BulkActionConfig> getBulkActions(@PathVariable(value = "versionId") final Integer findingAidVersionId) {
        Assert.notNull(findingAidVersionId);
        return bulkActionService.getBulkActions(findingAidVersionId, false);
    }

    @Override
    @RequestMapping(value = "/bulkactions/{versionId}/mandatory", method = RequestMethod.GET)
    public List<BulkActionConfig> getMandatoryBulkActions(@PathVariable(value = "versionId") final Integer findingAidVersionId) {
        Assert.notNull(findingAidVersionId);
        return bulkActionService.getBulkActions(findingAidVersionId, true);
    }

    @Override
    @RequestMapping(value = "/run/{versionId}", method = RequestMethod.POST)
    public void run(@RequestBody final BulkActionConfig bulkActionConfig,
                    @PathVariable(value = "versionId") final Integer findingAidVersionId) {
        Assert.notNull(bulkActionConfig);
        Assert.notNull(findingAidVersionId);
        bulkActionService.run(bulkActionConfig, findingAidVersionId);
    }

    @Override
    @RequestMapping(value = "/validate/{versionId}", method = RequestMethod.GET)
    public List<BulkActionConfig> runValidation(@PathVariable(value = "versionId") final Integer findingAidVersionId) {
        Assert.notNull(findingAidVersionId);
        return bulkActionService.runValidation(findingAidVersionId);
    }

}
