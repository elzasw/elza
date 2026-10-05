package cz.tacr.elza.service.da;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.archivists.ead3.schema.Ead;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import gov.loc.mets.v1_11.schema.MdSecType;
import gov.loc.mets.v1_11.schema.MetsType;
import jakarta.transaction.Transactional;

/**
 * Imports the description a package of the digital archive carries into the archival
 * description of its fund: reads the stored metadata package, plans the import by the rules of
 * the fund ({@link DaImportPlanner}) and carries the plan out ({@link DaImportBuilder}).
 */
@Service
public class DaImportService {

    /** GROUPID of the dmdSec of the contextual description; any other is the inherent one. */
    private static final String CONTEXTUAL = "CONTEXTUAL";

    private final DaService daService;
    private final DaImportPlanner planner;
    private final DaImportBuilder builder;
    private final AipStateRepository aipStateRepository;
    private final DaLocalCacheRepository localCacheRepository;
    private final ArrangementInternalService arrangementInternalService;
    private final NodeRepository nodeRepository;
    private final ArrDaLinkRepository daLinkRepository;
    private final DaDaoRepository daoRepository;

    public DaImportService(DaService daService, DaImportPlanner planner, DaImportBuilder builder,
                           AipStateRepository aipStateRepository, DaLocalCacheRepository localCacheRepository,
                           ArrangementInternalService arrangementInternalService, NodeRepository nodeRepository,
                           ArrDaLinkRepository daLinkRepository, DaDaoRepository daoRepository) {
        this.daService = daService;
        this.planner = planner;
        this.builder = builder;
        this.aipStateRepository = aipStateRepository;
        this.localCacheRepository = localCacheRepository;
        this.arrangementInternalService = arrangementInternalService;
        this.nodeRepository = nodeRepository;
        this.daLinkRepository = daLinkRepository;
        this.daoRepository = daoRepository;
    }

    /**
     * Imports what lies below a div of the logical structural map of the package that is
     * described already - the node it was matched onto by its UUID. The node itself is taken as
     * it is.
     *
     * @param divUuid UUID of the div, as {@link AipNodeUuids#normalize(String)} gives it
     * @return what the import did; empty when the rules of the fund cannot import packages, or
     *         the UUID is not of a div of the logical structural map
     * @throws AipProblemException when the package cannot be read or its EAD is not written the
     *             way it can be read
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<DaImportBuilder.Outcome> importBelow(DaAip aip, ArrNode node, String divUuid,
                                                         boolean fileplanAsRoot) {
        return withPackage(aip, node, fileplanAsRoot, (pkg, ruleSetId) ->
                planner.planBelow(pkg.mets(), pkg.ead(), pkg.eadHref(), ruleSetId, pkg.importPackage(), divUuid))
                .map(plan -> builder.build(aip, node, plan));
    }

    /**
     * Places a received package that matches no unit of description by UUID into the archival
     * description, as the DA_MATCH script of the rules of the fund decides
     * ({@link DaImportPlanner#planPlacement}): the levels it is placed under are found or created
     * below the root of the fund, and the package is imported below them or linked to them.
     *
     * @param root the root of the fund of the package
     * @return what the placement did; empty when the rules of the fund have no DA_MATCH script, or
     *         the script leaves the package to a user
     * @throws AipProblemException when the package cannot be read or its EAD is not written the
     *             way it can be read
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<DaImportBuilder.Outcome> placeReceived(DaAip aip, ArrNode root) {
        return withPackage(aip, root, false, (pkg, ruleSetId) ->
                planner.planPlacement(pkg.mets(), pkg.ead(), pkg.eadHref(), ruleSetId, pkg.importPackage()))
                .map(plan -> builder.build(aip, root, plan));
    }

    /**
     * Imports the description the package carries - its whole logical structural map - below the
     * given unit of description, as asked for by a user
     * ({@link cz.tacr.elza.api.DaAipActionType#IMPORT_DESCRIPTION}).
     *
     * @throws BusinessException when the package cannot be imported there: it belongs to another
     *             fund, it is attached already, or the rules of the fund cannot import packages
     * @throws AipProblemException when the package cannot be read or its EAD is not written the
     *             way it can be read
     */
    @Transactional
    public DaImportBuilder.Outcome importPackage(Integer aipId, Integer nodeId, boolean fileplanAsRoot) {
        return importDescription(aipId, nodeId, null, null, fileplanAsRoot, false);
    }

    /**
     * Imports what lies below a level of the logical structure of the package into the archival
     * description below the given unit of description, as asked for by a user.
     *
     * @param levelViewId the level (level view, shared by packages) below which the description
     *            is taken
     * @param daoId the level of the logical structure of this package below which the description
     *            is taken - used when a single package is imported; with neither of the two, the top
     *            of the package - the whole package
     * @param firstLevelOnly only the levels directly below the level are created, each with its
     *            part of the package attached ({@link cz.tacr.elza.api.DaAipActionType#CREATE_SUBLEVELS});
     *            otherwise the whole structure below it, with the items from the EAD
     *            ({@link cz.tacr.elza.api.DaAipActionType#IMPORT_DESCRIPTION})
     * @throws BusinessException when the package cannot be imported there: it belongs to another
     *             fund, it does not have the level, the whole package is attached already, or the
     *             rules of the fund cannot import packages (the first level can be created
     *             without them, the levels then carry no items)
     * @throws AipProblemException when the package cannot be read or its EAD is not written the
     *             way it can be read
     */
    @Transactional
    public DaImportBuilder.Outcome importDescription(Integer aipId, Integer nodeId, @Nullable Integer levelViewId,
                                                     @Nullable Integer daoId, boolean fileplanAsRoot,
                                                     boolean firstLevelOnly) {
        DaAip aip = daService.findAipById(aipId);
        ArrNode node = nodeRepository.getOneCheckExist(nodeId);
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        if (aipState == null || aipState.getFund() == null
                || !aipState.getFund().getFundId().equals(node.getFundId())) {
            throw new BusinessException("AIP " + aip.getCode() + " nepatří k archivnímu souboru, do kterého se má popis převzít.",
                                        BaseCode.INVALID_STATE);
        }
        String startUuid = daoId != null ? daoUuid(aip, daoId)
                : levelViewId != null ? levelUuid(aip, levelViewId) : null;
        if (startUuid == null && !firstLevelOnly
                && !daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).isEmpty()) {
            throw new BusinessException("AIP " + aip.getCode() + " je již připojen k archivnímu popisu; popis z něj se znovu nepřebírá.",
                                        ArrangementCode.DAO_ALREADY_LINKED);
        }
        return withPackage(aip, node, fileplanAsRoot, (pkg, ruleSetId) -> {
                    // levels only (without items) need no rules; the description does
                    if (!firstLevelOnly && !planner.canImport(ruleSetId)) {
                        throw new BusinessException("Pravidla archivního souboru (" + planner.ruleSetLabel(ruleSetId)
                                + ") převzetí popisu z balíčku nepodporují - chybí v nich pravidlo DA_IMPORT.",
                                BaseCode.INVALID_STATE);
                    }
                    Optional<DaImportPlan> plan = startUuid == null
                            ? planner.plan(pkg.mets(), pkg.ead(), pkg.eadHref(), ruleSetId, pkg.importPackage())
                            : planner.planBelow(pkg.mets(), pkg.ead(), pkg.eadHref(), ruleSetId, pkg.importPackage(),
                                                startUuid);
                    if (plan.isEmpty() && firstLevelOnly) {
                        // without rules the levels are created without items
                        plan = Optional.of(planner.planDivsBelow(pkg.mets(), startUuid));
                    }
                    return firstLevelOnly ? plan.map(DaImportPlan::firstLevel) : plan;
                })
                .map(plan -> builder.build(aip, node, plan))
                // with rules, only the start div can be missing: the part is not in the logical structural map
                .orElseThrow(() -> new BusinessException("Vybraná úroveň (" + startUuid + ") v logické struktuře METS balíčku "
                                                         + aip.getCode() + " není.", BaseCode.INVALID_STATE));
    }

    /** UUID of the div of the package that stands for the level (level view). */
    private String levelUuid(DaAip aip, Integer levelViewId) {
        return daoRepository.findByAipAndTypeAndDeleteChangeIsNull(aip, DaDao.DaoType.LOGICAL).stream()
                .filter(dao -> dao.getLevelView() != null && levelViewId.equals(dao.getLevelView().getLevelViewId()))
                .map(dao -> AipNodeUuids.normalize(dao.getCode()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new BusinessException("AIP " + aip.getCode() + " vybranou úroveň logické struktury neobsahuje.",
                                                         BaseCode.INVALID_STATE));
    }

    /** UUID of the div of the package that the logical part stands for. */
    private String daoUuid(DaAip aip, Integer daoId) {
        DaDao dao = daoRepository.findById(daoId).orElse(null);
        String uuid = dao == null ? null : AipNodeUuids.normalize(dao.getCode());
        if (dao == null || uuid == null || dao.getType() != DaDao.DaoType.LOGICAL
                || !dao.getAip().getAipId().equals(aip.getAipId())) {
            throw new BusinessException("Vybraná část není úrovní logické struktury AIP " + aip.getCode() + ".",
                                        BaseCode.INVALID_STATE);
        }
        return uuid;
    }

    /** What the planning needs of a stored metadata package. */
    private record OpenPackage(MetsType mets, @Nullable Ead ead, @Nullable String eadHref,
                               DaImportPackage importPackage) {
    }

    private <R> Optional<R> withPackage(DaAip aip, ArrNode node, boolean fileplanAsRoot,
                                        BiFunction<OpenPackage, Integer, Optional<R>> planning) {
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        ArrFundVersion version = arrangementInternalService.getOpenVersionByFund(node.getFund());
        DaImportPackage importPackage = new DaImportPackage(aipState.getContentType(), aipState.getProfile(),
                fileplanAsRoot);

        Path dir = unpack(aipState);
        try {
            MetsType mets = readMets(dir);
            String eadHref = inherentDescriptionHref(mets);
            Ead ead = eadHref == null ? null : readEad(dir, eadHref);
            return planning.apply(new OpenPackage(mets, ead, eadHref, importPackage), version.getRuleSetId());
        } finally {
            DaService.deleteTempDirectory(dir);
        }
    }

    private Path unpack(DaAipState aipState) {
        DaLocalCache localCache = localCacheRepository.findByAipStateAndAipTypeIn(aipState,
                EnumSet.of(AipType.METADATA_BASE, AipType.AIP_BASE), DaService.getQueueImportStates());
        if (localCache == null) {
            throw AipProblemException.metadata("V ELZA není uložený balíček s metadaty, ze kterého by šlo popis převzít.");
        }
        String file = localCache.getFilePathMetadata() != null ? localCache.getFilePathMetadata()
                : localCache.getFilePath();
        try {
            return daService.unpack(Paths.get(file));
        } catch (Exception e) {
            throw new SystemException("Uložený balíček s metadaty se nepodařilo rozbalit: " + file, e,
                                      BaseCode.SYSTEM_ERROR);
        }
    }

    private MetsType readMets(Path dir) {
        try {
            return daService.readMets(dir);
        } catch (AipProblemException e) {
            throw e;
        } catch (Exception e) {
            throw AipProblemException.metadata("Soubor METS.xml balíčku se nepodařilo přečíst: " + AipProblem.reason(e),
                                               "METS.xml", e);
        }
    }

    private Ead readEad(Path dir, String href) {
        try {
            return daService.loadEadFile(dir, href.replace("/", File.separator));
        } catch (AipProblemException e) {
            throw e;
        } catch (Exception e) {
            throw AipProblemException.metadata("Inherentní archivní popis '" + href + "' se nepodařilo načíst: "
                    + AipProblem.reason(e), href, e);
        }
    }

    /** Path of the inherent archival description inside the package; null when it has none. */
    @Nullable
    static String inherentDescriptionHref(MetsType mets) {
        for (MdSecType dmdSec : mets.getDmdSec()) {
            if (!CONTEXTUAL.equals(dmdSec.getGROUPID()) && dmdSec.getMdRef() != null
                    && ("CURRENT".equals(dmdSec.getSTATUS()) || dmdSec.getSTATUS() == null)) {
                return dmdSec.getMdRef().getHref();
            }
        }
        return null;
    }
}
