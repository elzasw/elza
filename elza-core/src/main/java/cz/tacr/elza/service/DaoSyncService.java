package cz.tacr.elza.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.types.v1.ChecksumType;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.Dids;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.FileGroup;
import cz.tacr.elza.ws.types.v1.NonexistingDaos;
import cz.tacr.elza.ws.types.v1.RelatedFileGroup;
import cz.tacr.elza.ws.types.v1.UnitOfMeasure;

import static java.util.stream.Collectors.*;

/**
 * Servisní metody pro synchronizaci digitizátů.
 */
@Service
public class DaoSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DaoSyncService.class);

    /**
     * Velikost dávky pro synchronizaci.
     */
    private static final int SYNC_DAOS_BATCH_SIZE = 100;

    // --- dao ---

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    // --- services ---

    @Autowired
    private DaoService daoService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserService userService;

    // --- fields ---

    @Autowired
    private WsClient wsClient;

    @PersistenceContext
    private EntityManager entityManager;

    // --- methods ---

    /**
     * Zavolá WS pro synchronizaci digitalizátů a aktualizuje metadata pro daný node a DAO.
     *
     * @param fundVersionId verze AS
     * @param dao           DAO pro synchronizaci
     * @param node          node pro synchronizaci
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void syncDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrNode node, ArrDao dao) {

        DaosSyncRequest daosSyncRequest = createDaosSyncRequest(fundVersion, node, dao);

        ArrDigitalRepository digitalRepository = dao.getDaoPackage().getDigitalRepository();
        DaosSyncResponse daosSyncResponse = wsClient.syncDaos(daosSyncRequest, digitalRepository);

        processDaosSyncResponse(daosSyncResponse);
    }

    public DaosSyncRequest createDaosSyncRequest(ArrFundVersion fundVersion, ArrNode node, ArrDao dao) {
        DaoSyncRequest request = createDaoSyncRequest(node, dao);
        return createDaosSyncRequest(fundVersion, Collections.singletonList(request));
    }

    public DaosSyncRequest createDaosSyncRequest(ArrDaoRequest request) {

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(request.getFund().getFundId());
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nenalezena otevřená verze AS", ArrangementCode.FUND_VERSION_NOT_FOUND)
                    .setId(request.getFund().getFundId());
        }

        List<ArrDao> daos = daoRequestDaoRepository.findDaoByDaoRequest(request);

        List<ArrDaoLink> arrDaoLinks = daoLinkRepository.findActiveByDaos(daos);

        List<DaoSyncRequest> list = arrDaoLinks.stream()
                .map(arrDaoLink -> createDaoSyncRequest(arrDaoLink.getNode(), arrDaoLink.getDao()))
                .collect(toList());

        return createDaosSyncRequest(fundVersion, list);
    }

    private DaosSyncRequest createDaosSyncRequest(ArrFundVersion fundVersion, List<DaoSyncRequest> list) {

        DaosSyncRequest request = new DaosSyncRequest();
        request.setDids(new Dids());
        request.setFundIdentifier(fundVersion.getRootNode().getUuid());
        request.setUsername(getUsername());

        request.getDaoSyncRequest().addAll(list);

        return request;
    }

    private DaoSyncRequest createDaoSyncRequest(ArrNode node, ArrDao dao) {
        DaoSyncRequest daoSyncRequest = new DaoSyncRequest();
        daoSyncRequest.setDidId(node.getUuid());
        daoSyncRequest.setDaoId(dao.getCode());
        return daoSyncRequest;
    }

    /**
     * Provede aktualizaci metadat.
     * @param daosSyncResponse response z WS {@code syncDaos}
     */
    public void processDaosSyncResponse(DaosSyncResponse daosSyncResponse) {
        deleteDaos(daosSyncResponse.getNonexistingDaos());
        updateDaos(daosSyncResponse.getDaoset());
    }

    private void deleteDaos(NonexistingDaos nonexistingDaos) {
        if (nonexistingDaos != null) {
            List<String> daoCodes = nonexistingDaos.getDaoId();
            if (!daoCodes.isEmpty()) {
                List<ArrDao> arrDaos = daoRepository.findByCodes(daoCodes);
                daoService.deleteDaos(arrDaos, false);
            }
        }
    }

    private void updateDaos(Daoset daoset) {
        if (daoset == null) {
            return;
        }

        List<Dao> daoList = daoset.getDao();
        if (daoList.isEmpty()) {
            return;
        }

        Map<String, ArrDao> daoCache = daoRepository.findByCodes(daoList.stream().map(dao -> dao.getIdentifier()).collect(toList()))
                .stream().collect(toMap(dao -> dao.getCode(), dao -> dao));

        for (Dao dao : daoList) {

            ArrDao arrDao = daoCache.get(dao.getIdentifier());
            if (arrDao != null) {
                arrDao.setLabel(dao.getLabel());
                arrDao.setValid(true);
                arrDao = daoRepository.save(arrDao);
            } else {
                logger.warn("Neplatné DAO [code=\"" + dao.getIdentifier() + "]");
            }

            updateFiles(dao.getFileGroup());

            updateRelatedFileGroup(dao.getRelatedFileGroup());
        }
    }

    private void updateFiles(FileGroup fileGroup) {
        if (fileGroup == null) {
            return;
        }

        List<File> fileList = fileGroup.getFile();
        if (fileList.isEmpty()) {
            return;
        }

        Map<String, ArrDaoFile> fileCache = daoFileRepository.findByCodes(fileList.stream().map(file -> file.getIdentifier()).collect(toList()))
                .stream().collect(toMap(file -> file.getCode(), file -> file));

        for (File file : fileList) {
            ArrDaoFile arrDaoFile = fileCache.get(file.getIdentifier());
            if (arrDaoFile != null) {
                updateArrDaoFile(arrDaoFile, file);
                daoFileRepository.save(arrDaoFile);
            } else {
                logger.warn("Neplatný DAO file [code=\"" + file.getIdentifier() + "]");
            }
        }
    }

    private void updateRelatedFileGroup(List<RelatedFileGroup> relatedFileGroupList) {
        if (relatedFileGroupList == null) {
            return;
        }
        if (relatedFileGroupList.isEmpty()) {
            return;
        }

        Map<String, ArrDaoFileGroup> groupCache = daoFileGroupRepository.findByCodes(relatedFileGroupList.stream().map(group -> group.getIdentifier()).collect(toList()))
                .stream().collect(toMap(group -> group.getCode(), group -> group));

        for (RelatedFileGroup relatedFileGroup : relatedFileGroupList) {

            updateFiles(relatedFileGroup.getFileGroup());
        }
    }

    /**
     * Spustí asynchronní synchronizaci digitalizátů a aktualizuje metadata pro všechny nody z AS, které mají připojené DAO.
     *
     * @param fundVersionId verze AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void syncDaosAll(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        int i = 0;

        for (Map.Entry<Integer, List<Integer>> entry : groupDaoIdsByDigitalRepository(fundVersionId).entrySet()) {

            Integer externalSystemId = entry.getKey();

            for (List<Integer> daoIds : Lists.partition(entry.getValue(), SYNC_DAOS_BATCH_SIZE)) {

                syncDaos(externalSystemId, fundVersionId, daoIds);
                entityManager.flush();

                // promazat hibernate session po zpracovani vetsiho mnozstvi zaznamu
                i += daoIds.size();
                if (i > 2000) {
                    entityManager.clear();
                    i = 0;
                }
            }
        }
    }

    private Map<Integer, List<Integer>> groupDaoIdsByDigitalRepository(@NotNull Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        return daoRepository.findValidDaoExternalSystemByFund(fundVersion.getFund().getFundId()).stream()
                .collect(groupingBy(vo -> vo.getExternalSystemId(), mapping(vo -> vo.getDaoId(), toList())));
    }

    private void syncDaos(@NotNull Integer externalSystemId, @NotNull Integer fundVersionId, @NotNull List<Integer> daoIds) {

        ArrDigitalRepository digitalRepository = digitalRepositoryRepository.getOneCheckExist(externalSystemId);

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);

        List<ArrDao> daos = daoRepository.findAll(daoIds);

        // natahnout packages do hibernate session, protoze v createDaoRequest se package z DAO pouzivaji
        daoPackageRepository.findAll(daos.stream().map(dao -> dao.getDaoPackage().getDaoPackageId()).collect(toSet()));

        ArrDaoRequest daoRequest = requestService.createDaoRequest(daos, null, ArrDaoRequest.Type.SYNC, fundVersion, digitalRepository);

        requestService.sendRequest(daoRequest, fundVersion);
    }

    public ArrDao createArrDao(ArrDaoPackage arrDaoPackage, Dao dao) {
        if (StringUtils.isBlank(dao.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("dao.identifier", dao.getIdentifier());
        }
        ArrDao arrDao = new ArrDao();
        arrDao.setCode(dao.getIdentifier());
        arrDao.setLabel(dao.getLabel());
        arrDao.setValid(true);
        arrDao.setDaoPackage(arrDaoPackage);
        return daoRepository.save(arrDao);
    }

    public ArrDaoFileGroup createArrDaoFileGroup(ArrDao arrDao, RelatedFileGroup relatedFileGroup) {
        if (StringUtils.isBlank(relatedFileGroup.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("relatedFileGroup.identifier", relatedFileGroup.getIdentifier());
        }
        ArrDaoFileGroup arrDaoFileGroup = new ArrDaoFileGroup();
        arrDaoFileGroup.setCode(relatedFileGroup.getIdentifier());
        arrDaoFileGroup.setLabel(relatedFileGroup.getLabel());
        arrDaoFileGroup.setDao(arrDao);
        return daoFileGroupRepository.save(arrDaoFileGroup);
    }

    public ArrDaoFile createArrDaoFileGroup(ArrDaoFileGroup arrDaoFileGroup, File file) {
        if (StringUtils.isBlank(file.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("file.identifier", file.getIdentifier());
        }
        ArrDaoFile arrDaoFile = new ArrDaoFile();
        arrDaoFile.setCode(file.getIdentifier());
        arrDaoFile.setDaoFileGroup(arrDaoFileGroup);
        updateArrDaoFile(arrDaoFile, file);
        return daoFileRepository.save(arrDaoFile);
    }

    public ArrDaoFile createArrDaoFile(ArrDao arrDao, File file) {
        if (StringUtils.isBlank(file.getIdentifier())) {
            throw new BusinessException("Nebylo vyplněno povinné pole identifikátoru", DigitizationCode.NOT_FILLED_EXTERNAL_IDENTIRIER)
                    .set("file.identifier", file.getIdentifier());
        }
        ArrDaoFile arrDaoFile = new ArrDaoFile();
        arrDaoFile.setCode(file.getIdentifier());
        arrDaoFile.setDao(arrDao);
        updateArrDaoFile(arrDaoFile, file);
        return daoFileRepository.save(arrDaoFile);
    }

    public void updateArrDaoFile(ArrDaoFile arrDaoFile, File file) {
        if (file.getChecksumType() != null) {
            arrDaoFile.setChecksumType(getChecksumType(file.getChecksumType()));
        }
        arrDaoFile.setChecksum(file.getChecksum());
        if (file.getCreated() != null) {
            arrDaoFile.setCreated(file.getCreated().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
        }
        arrDaoFile.setMimetype(file.getMimetype());
        arrDaoFile.setSize(file.getSize());
        if (file.getImageHeight() != null) {
            arrDaoFile.setImageHeight(file.getImageHeight().intValueExact());
        }
        if (file.getImageHeight() != null) {
            arrDaoFile.setImageWidth(file.getImageWidth().intValueExact());
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceXDimesionUnit(getDimensionUnit(file.getSourceXDimensionUnit()));
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceXDimesionValue(file.getSourceXDimensionValue().doubleValue());
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceYDimesionUnit(getDimensionUnit(file.getSourceYDimensionUnit()));
        }
        if (file.getSourceXDimensionUnit() != null) {
            arrDaoFile.setSourceYDimesionValue(file.getSourceYDimensionValue().doubleValue());
        }
        arrDaoFile.setDuration(file.getDuration());
    }

    private cz.tacr.elza.api.UnitOfMeasure getDimensionUnit(final UnitOfMeasure sourceDimensionUnit) {
        if (sourceDimensionUnit == null) {
            return null;
        }
        switch (sourceDimensionUnit) {
            case IN:
                return cz.tacr.elza.api.UnitOfMeasure.IN;
            case MM:
                return cz.tacr.elza.api.UnitOfMeasure.MM;
            default:
                throw new BusinessException("Neplatná jednotka: " + sourceDimensionUnit, PackageCode.PARSE_ERROR);
        }
    }

    private ArrDaoFile.ChecksumType getChecksumType(final ChecksumType checksumType) {
        if (checksumType == null) {
            return null;
        }
        switch (checksumType) {
            case MD_5:
                return ArrDaoFile.ChecksumType.MD5;
            case SHA_1:
                return ArrDaoFile.ChecksumType.SHA1;
            case SHA_256:
                return ArrDaoFile.ChecksumType.SHA256;
            case SHA_384:
                return ArrDaoFile.ChecksumType.SHA384;
            case SHA_512:
                return ArrDaoFile.ChecksumType.SHA512;
            default:
                throw new BusinessException("Neplatný checksum typ: " + checksumType, PackageCode.PARSE_ERROR);
        }
    }

    private String getUsername() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail != null && userDetail.getId() != null) {
            return userDetail.getUsername();
        }
        return null;
    }
}
