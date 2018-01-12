package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang.Validate;

import com.google.common.collect.Iterables;

import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.DEExportException;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.AccessPointsOutputStream;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.UserService;

/**
 * Reads access points specified by context.<br>
 * <i>Note: Implementation does not export coordinates, which will be most likely removed in
 * future.</i>
 */
public class AccessPointsReader implements ExportReader {

    // TODO: delete after removal of hierarchy
    private final ListValuedMap<Integer, RegRecord> childQueue = new ArrayListValuedHashMap<>();

    private final Set<Integer> exportedAPIds = new HashSet<>();

    private final Set<Integer> authorizedScopeIds = new HashSet<>();

    private final ExportContext context;

    private final EntityManager em;

    private final RegRecordRepository recordRepository;

    private final UserService userService;

    private final VariantNameLoader variantNameLoader;

    private final ExternalSystemLoader externalSystemLoader;

    public AccessPointsReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.em = initHelper.getEntityManager();
        this.recordRepository = initHelper.getRecordRepository();
        this.userService = initHelper.getUserService();
        this.variantNameLoader = new VariantNameLoader(em, context.getBatchSize());
        this.externalSystemLoader = new ExternalSystemLoader(em, context.getBatchSize());
    }

    /**
     * Reads all access points. Party access points are passed to <code>PartiesReader</code>.
     */
    @Override
    public void read() {
        AccessPointsOutputStream os = context.getBuilder().openAccessPointsOutputStream();
        try {
            for (List<Integer> apIds : Iterables.partition(context.getAPIds(), context.getBatchSize())) {
                readAccessPoints(apIds, os);
            }
            Validate.isTrue(childQueue.isEmpty());
            os.processed();
        } finally {
            childQueue.clear();
            exportedAPIds.clear();
            authorizedScopeIds.clear();
            os.close();
        }
    }

    private void readAccessPoints(Collection<Integer> apIds, AccessPointsOutputStream os) {
        // TODO: replace findAccessPointsWithParents with loader after removal of hierarchy
        List<RegRecord> apWithParents = recordRepository.findAccessPointsWithParents(apIds);
        List<RegRecord> batch = new ArrayList<>(context.getBatchSize());

        boolean globalPermission = userService.hasPermission(Permission.REG_SCOPE_RD_ALL);

        int rootCount = 0;

        for (RegRecord ap : apWithParents) {
            em.detach(ap); // TODO: replace detach for stateless session

            // check permission
            Integer scopeId = ap.getScopeId();
            if (!globalPermission && authorizedScopeIds.add(scopeId)) {
                if (!userService.hasPermission(Permission.REG_SCOPE_RD, scopeId)) {
                    throw Authorization.createAccessDeniedException(Permission.REG_SCOPE_RD);
                }
            }
            // increment root count
            if (ap.getParentRecordId() == null) {
                rootCount++;
            }

            boolean process = readAP(ap);
            if (process) {
                addAccessPoint(ap, batch, os);
                childQueue.remove(ap.getRecordId()).forEach(child -> {
                    addAccessPoint(child, batch, os);
                });
            }
        }

        if (batch.size() > 0) {
            processBatch(batch, os);
        }
        if (rootCount != apIds.size()) {
            throw new DEExportException("Not all access points were found");
        }
    }

    /**
     * Prepare access point for batch.
     *
     * @return True when access point can processed.
     */
    private boolean readAP(RegRecord ap) {
        // set register type relation
        RegRegisterType rt = context.getStaticData().getRegisterTypeById(ap.getRegisterTypeId());
        ap.setRegisterType(rt);

        // check party AP
        if (rt.getPartyType() != null) {
            context.addPartyAPId(ap.getRecordId());
            return false;
        }

        // check exported (can occur because of hierarchy)
        if (!exportedAPIds.add(ap.getRecordId())) {
            return false;
        }

        // check exported parent
        Integer parentAPId = ap.getParentRecordId();
        if (parentAPId != null && !exportedAPIds.contains(parentAPId)) {
            childQueue.put(parentAPId, ap);
            return false;
        }
        return true;
    }

    private void addAccessPoint(RegRecord ap, List<RegRecord> batch, AccessPointsOutputStream os) {
        variantNameLoader.addRequest(ap.getRecordId(), new VariantNameDispatcher(ap));
        if (ap.getExternalSystemId() != null) {
            externalSystemLoader.addRequest(ap.getExternalSystemId(), new ExternalSystemDispatcher(ap));
        }

        batch.add(ap);

        if (batch.size() >= context.getBatchSize()) {
            processBatch(batch, os);
        }
    }

    private void processBatch(List<RegRecord> batch, AccessPointsOutputStream os) {
        variantNameLoader.flush();
        externalSystemLoader.flush();
        batch.forEach(os::addAccessPoint);
        batch.clear();
    }
}
