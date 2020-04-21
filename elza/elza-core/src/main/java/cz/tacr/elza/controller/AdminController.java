package cz.tacr.elza.controller;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.AsyncTypeEnum;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.*;

/**
 * Kontroler pro administraci.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Value("${elza.logFile:}")
    private String logFilePath;

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

    @RequestMapping(value="/asyncRequests", method = RequestMethod.GET)
    public List<ArrAsyncRequestVO> getAsyncRequestInfo() {
        return asyncRequestService.dispatcherInfo();
    }

    @RequestMapping(value= "/asyncRequests/{requestType}", method = RequestMethod.GET)
    public List<FundStatisticsVO> getAsyncRequestDetail(@PathVariable("requestType") AsyncTypeEnum requestType) {
        return asyncRequestService.getFundStatistics(requestType);
    }

    @RequestMapping(value = "/logs", method = RequestMethod.GET)
    public LogVO getLogs(@RequestParam(name = "lineCount", required = false, defaultValue = "1000") Integer lineCount) {
        List<String> lines = new ArrayList<>(lineCount);

        try {
            if (StringUtils.isBlank(logFilePath)) {
                lines.add("Chyba konfigurace, není nastavena cesta k souboru logu.");
            } else {
                FileInputStream fileInputStream = new FileInputStream(new File(logFilePath));
                FileChannel channel = fileInputStream.getChannel();
                ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                buffer.position((int) channel.size());
                int count = 0;
                byte[] lineArray = new byte[0];
                for (long i = channel.size() - 1; i >= 0; i--) {
                    byte c = buffer.get((int) i);
                    if (c == '\n') {
                        ArrayUtils.reverse(lineArray);
                        lines.add(new String(lineArray, "UTF8"));
                        lineArray = new byte[0];
                        if (count == lineCount) break;
                        count++;
                    } else {
                        lineArray = ArrayUtils.add(lineArray, c);
                    }
                }
                if (lineArray.length > 0) {
                    ArrayUtils.reverse(lineArray);
                    lines.add(new String(lineArray, "UTF8"));
                }
                channel.close();
            }
        } catch (FileNotFoundException e) {
            lines.add("Soubor logu " + logFilePath + " nebyl nalezen.");
            logger.error("Soubor logu " + logFilePath + " nebyl nalezen.", e);
        } catch (IOException e) {
            lines.add("Chyba při čtení souboru logu " + logFilePath + ".");
            logger.error("Chyba při čtení souboru logu " + logFilePath + ".", e);
        }

//        logger.info("Z logu načteno " + lineCount + " řádek.");

        LogVO result = new LogVO();
        Collections.reverse(lines);
        result.setLines(lines);
        result.setLineCount(lines.size());
        return result;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
}
