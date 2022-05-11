package cz.tacr.elza.ws.core.v1.daoservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;

/**
 * Context for dao import
 *
 */
public class ImportContext {

    Map<String, ArrFundVersion> fundVersionsByString = new HashMap<>();

    /**
     * Fund versions by fundId
     */
    Map<Integer, ArrFundVersion> fundVersionsByFund = new HashMap<>();

    final Map<Integer, ArrChange> changesMap = new HashMap<>();

    final Map<String, ArrDaoPackage> daoPackageMap = new HashMap<>();

    final List<ArrDaoPackage> daoPackages = new ArrayList<>();

    final List<ArrDao> daos = new ArrayList<>();

    final Map<String, ArrDao> daoMap = new HashMap<>();

    /**
     * Map of recommended scenarios for DAOs (by daoId)
     */
    final Map<Integer, String> recommendedScenarios = new HashMap<>();

    /**
     * Map daoId to the list of files
     */
    final Map<Integer, List<ArrDaoFile>> daoFilesMap = new HashMap<>();

    final private ArrDigitalRepository repository;

    final private ArrangementInternalService arrangementInternalService;

    final private DescriptionItemService descriptionItemService;

    /**
     * Map fundVersionId to itemChangeContext
     */
    final private Map<Integer, MultipleItemChangeContext> itemsChangeContextMap = new HashMap<>();

    public ArrDigitalRepository getRepository() {
        return repository;
    }

    public ImportContext(final ArrDigitalRepository repository,
                         final ArrangementInternalService arrangementInternalService,
                         final DescriptionItemService descriptionItemService) {
        this.repository = repository;
        this.arrangementInternalService = arrangementInternalService;
        this.descriptionItemService = descriptionItemService;
    }

    /**
     * Return change for given fund
     * 
     * @param fund
     * @return
     */
    public ArrChange getChange(final ArrFund fund) {
        return changesMap.computeIfAbsent(fund.getFundId(),
                                          f -> arrangementInternalService
                                                  .createChange(ArrChange.Type.IMPORT, null));
    }

    private void addPackage(final ArrDaoPackage daoPackage) {
        String daoPackageCode = daoPackage.getCode();
        if (!daoPackageMap.containsKey(daoPackageCode)) {
            daoPackages.add(daoPackage);
            daoPackageMap.put(daoPackageCode, daoPackage);
        }
    }

    public void addDao(ArrDao dao) {
        ArrDao prevDao = daoMap.put(dao.getCode(), dao);
        if (prevDao != null) {
            throw new SystemException("Multiple DAOs with same code.", BaseCode.SYSTEM_ERROR)
                    .set("code", dao.getCode());
        }
        daos.add(dao);

        ArrDaoPackage daoPackage = dao.getDaoPackage();
        addPackage(daoPackage);
    }

    public List<ArrDao> getDaos() {
        return daos;
    }

    public ArrFundVersion getFundVersion(String fundIdentifier) {
        ArrFundVersion fv = fundVersionsByString.get(fundIdentifier);
        if (fv == null) {
            fv = arrangementInternalService.getOpenVersionByString(fundIdentifier);
            fundVersionsByString.put(fundIdentifier, fv);
            fundVersionsByFund.put(fv.getFundId(), fv);
        }
        return fv;
    }

    public ArrFundVersion getFundVersion(ArrFund fund) {
        ArrFundVersion fv = fundVersionsByFund.get(fund.getFundId());
        if (fv == null) {
            fv = arrangementInternalService.getOpenVersionByFund(fund);
            fundVersionsByFund.put(fv.getFundId(), fv);
        }
        return fv;
    }

    public ArrDaoPackage getDaoPackage(String identifier) {
        return daoPackageMap.get(identifier);
    }

    public ArrDao getDao(String identifier) {
        return daoMap.get(identifier);
    }

    public void addDaos(List<ArrDao> daos) {
        for (ArrDao dbDao : daos) {
            addDao(dbDao);
        }

    }

    public List<ArrDaoFile> getFiles(ArrDao dbDao) {
        List<ArrDaoFile> daoFilesList = daoFilesMap.get(dbDao.getDaoId());
        return daoFilesList != null ? daoFilesList : Collections.emptyList();
    }

    public void addDaoFiles(List<ArrDaoFile> daoFiles) {
        for (ArrDaoFile daoFile : daoFiles) {
            addDaoFile(daoFile);
        }
    }

    public void addDaoFile(ArrDaoFile daoFile) {
        Integer daoId = daoFile.getDaoId();
        Validate.notNull(daoId);
        List<ArrDaoFile> daoFileList = daoFilesMap.computeIfAbsent(daoId, x -> new ArrayList<>());
        daoFileList.add(daoFile);
    }

    public void removeDaoFiles(Collection<ArrDaoFile> daoFiles) {
        for (ArrDaoFile daoFile : daoFiles) {
            List<ArrDaoFile> daoFilesList = daoFilesMap.get(daoFile.getDaoId());
            Validate.notNull(daoFilesList);
            daoFilesList.remove(daoFile);
            if (daoFilesList.isEmpty()) {
                daoFilesMap.remove(daoFile.getDaoId());
            }
        }

    }

    public MultipleItemChangeContext getItemsChangeContext(ArrFundVersion fundVersion) {
        MultipleItemChangeContext cc = itemsChangeContextMap.get(fundVersion.getFundVersionId());
        if (cc == null) {
            cc = descriptionItemService.createChangeContext(fundVersion.getFundVersionId());
            itemsChangeContextMap.put(fundVersion.getFundVersionId(), cc);
        }
        return cc;
    }

    public void setRecommendedScenario(ArrDao dao, String currScenario) {
        recommendedScenarios.put(dao.getDaoId(), currScenario);
    }

    public String getRecommendedScenario(ArrDao dao) {
        return recommendedScenarios.get(dao.getDaoId());
    }

    public void flush() {
        for (MultipleItemChangeContext cc : itemsChangeContextMap.values()) {
            cc.flush();
        }
    }

}
