package cz.tacr.elza.service.da;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import org.archivists.ead3.schema.Ead;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
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

    public DaImportService(DaService daService, DaImportPlanner planner, DaImportBuilder builder,
                           AipStateRepository aipStateRepository, DaLocalCacheRepository localCacheRepository,
                           ArrangementInternalService arrangementInternalService) {
        this.daService = daService;
        this.planner = planner;
        this.builder = builder;
        this.aipStateRepository = aipStateRepository;
        this.localCacheRepository = localCacheRepository;
        this.arrangementInternalService = arrangementInternalService;
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
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        ArrFundVersion version = arrangementInternalService.getOpenVersionByFund(node.getFund());
        DaImportPackage importPackage = new DaImportPackage(aipState.getContentType(), aipState.getProfile(),
                fileplanAsRoot);

        Path dir = unpack(aipState);
        try {
            MetsType mets = readMets(dir);
            String eadHref = inherentDescriptionHref(mets);
            Ead ead = eadHref == null ? null : readEad(dir, eadHref);
            return planner.planBelow(mets, ead, eadHref, version.getRuleSetId(), importPackage, divUuid)
                    .map(plan -> builder.build(aip, node, plan));
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
