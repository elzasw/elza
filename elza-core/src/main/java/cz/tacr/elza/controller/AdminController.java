package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.SysExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Kontroler pro administraci.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ArrangementService arrangementService;

    @RequestMapping(value = "/reindex", method = RequestMethod.GET)
	@Transactional
    public void reindex() {
        adminService.reindex();
    }

    @RequestMapping(value = "/reindexStatus", method = RequestMethod.GET)
	@Transactional
    public boolean reindexStatus() {
        return adminService.isIndexingRunning();
    }

    /**
     * Provede resetování všech cache na serveru.
     */
    @RequestMapping(value = "/cache/reset", method = RequestMethod.GET)
	@Transactional
    public void resetAllCache() {
        cacheService.resetAllCache();
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.GET)
	@Transactional
    public List<SysExternalSystemVO> findAllExternalSystems() {
        List<SysExternalSystem> extSystems = externalSystemService.findAll();
        return ApFactory.transformList(extSystems, factoryVo::createExtSystem);
    }

    /**
     * Vytvoří externí systém.
     *
     * @param externalSystemVO vytvářený externí systém
     * @return vytvořený externí systém
     */
    @RequestMapping(value = "/externalSystems", method = RequestMethod.POST)
    @Transactional
    public SysExternalSystemVO createExternalSystem(@RequestBody final SysExternalSystemVO externalSystemVO) {
        SysExternalSystem externalSystem = externalSystemVO.createEntity();
        // TODO: zvážit vytvoření specializovaných  metod pro konkrétní typy external system
        externalSystem = externalSystemService.create(externalSystem);
        return factoryVo.createExtSystem(externalSystem);
    }

    /**
     * Vyhledá externí systém podle identifikátoru.
     *
     * @param externalSystemId identifikátor externího systému
     * @return nalezený externí systém
     */
    @RequestMapping(value = "/externalSystems/{externalSystemId}", method = RequestMethod.GET)
	@Transactional
    public SysExternalSystemVO findExternalSystemById(@PathVariable("externalSystemId") final Integer externalSystemId) {
        SysExternalSystem extSystem = externalSystemService.findOne(externalSystemId);
        return factoryVo.createExtSystem(extSystem);
    }

    /**
     * Upravení externího systému.
     *
     * @param externalSystemVO upravovaný externí systém
     * @return upravený externí systém
     */
    @RequestMapping(value = "/externalSystems/{externalSystemId}", method = RequestMethod.PUT)
    @Transactional
    public SysExternalSystemVO updateExternalSystem(@RequestBody final SysExternalSystemVO externalSystemVO) {
        SysExternalSystem externalSystem = externalSystemVO.createEntity();
        externalSystem = externalSystemService.update(externalSystem);
        return factoryVo.createExtSystem(externalSystem);
    }

    /**
     * Smazání externího systému.
     *
     * @param externalSystemId identifikátor externího systému
     */
    @RequestMapping(value = "/externalSystems/{externalSystemId}", method = RequestMethod.DELETE)
    @Transactional
    public void deleteExternalSystemById(@PathVariable("externalSystemId") final Integer externalSystemId) {
        externalSystemService.delete(externalSystemId);
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    @RequestMapping(value = "/externalSystems/simple", method = RequestMethod.GET)
	@Transactional
    public List<SysExternalSystemSimpleVO> findAllExternalSystemsSimple() {
        List<SysExternalSystem> extSystems = externalSystemService.findAllWithoutPermission();
        return ApFactory.transformList(extSystems, factoryVo::createExtSystemSimple);
    }

    /**
     * Získání JP podle identifikátorů pro zobrazení.
     *
     * @param fundId  identifikátor AS
     * @param nodeIds identifikátory JP
     * @return seznam JP
     */
    @RequestMapping(value = "/{fundId}/nodes/byIds", method = RequestMethod.POST)
    @Transactional
    public List<TreeNodeVO> findNodeByIds(@PathVariable("fundId") Integer fundId,
                                          @RequestBody List<Integer> nodeIds) {
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fundId);
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nenalezena otevřená verze AS", ArrangementCode.FUND_VERSION_NOT_FOUND)
                    .setId(fundId);
        }
        return adminService.findNodeByIds(fundVersion, nodeIds);
    }
}
