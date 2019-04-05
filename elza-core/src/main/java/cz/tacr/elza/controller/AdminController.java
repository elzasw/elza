package cz.tacr.elza.controller;

import java.util.List;

import javax.transaction.Transactional;

import cz.tacr.elza.common.FactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.SysExternalSystemSimpleVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.service.AdminService;
import cz.tacr.elza.service.CacheService;
import cz.tacr.elza.service.ExternalSystemService;

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
        return FactoryUtils.transformList(extSystems, factoryVo::createExtSystem);
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
        return FactoryUtils.transformList(extSystems, factoryVo::createExtSystemSimple);
    }
}
