package cz.tacr.elza.dataexchange.output.aps;

import com.google.common.collect.Iterables;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.DEExportException;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.AccessPointsOutputStream;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.vo.ApAccessPointData;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads access points specified by context.<br>
 * <i>Note: Implementation does not export coordinates, which will be most likely removed in
 * future.</i>
 */
public class AccessPointsReader implements ExportReader {
    private final Set<Integer> exportedAPIds = new HashSet<>();

    private final Set<Integer> authorizedScopeIds = new HashSet<>();

    private final ExportContext context;

    private final EntityManager em;

    private final ApAccessPointRepository accessPointRepository;

    private final UserService userService;

    private final VariantNameLoader variantNameLoader;

    private final ExternalSystemLoader externalSystemLoader;

    private final AccessPointDataService accessPointDataService;

    public AccessPointsReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.em = initHelper.getEntityManager();
        this.accessPointRepository = initHelper.getAccessPointRepository();
        this.userService = initHelper.getUserService();
        this.variantNameLoader = new VariantNameLoader(em, context.getBatchSize());
        this.externalSystemLoader = new ExternalSystemLoader(em, context.getBatchSize());
        this.accessPointDataService = initHelper.getAccessPointDataService();
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
            os.processed();
        } finally {
            exportedAPIds.clear();
            authorizedScopeIds.clear();
            os.close();
        }
    }

    private void readAccessPoints(Collection<Integer> apIds, AccessPointsOutputStream os) {
        // TODO: replace findAccessPointsWithParents with loader after removal of hierarchy
        List<ApAccessPoint> apWithParents = accessPointRepository.findByAccessPointIdIn(apIds);
        List<ApAccessPoint> batch = new ArrayList<>(context.getBatchSize());

        boolean globalPermission = userService.hasPermission(Permission.AP_SCOPE_RD_ALL);

        int rootCount = 0;

        Map<Integer, ApAccessPointData> accessPointDataMap = accessPointDataService.mapAccessPointDataById(apIds);

        for (ApAccessPoint ap : apWithParents) {
            em.detach(ap); // TODO: replace detach for stateless session

            // check permission
            Integer scopeId = ap.getScopeId();
            if (!globalPermission && authorizedScopeIds.add(scopeId)) {
                if (!userService.hasPermission(Permission.AP_SCOPE_RD, scopeId)) {
                    throw Authorization.createAccessDeniedException(Permission.AP_SCOPE_RD);
                }
            }
            // increment root count
            rootCount++;

            boolean process = readAP(ap);
            if (process) {
                addAccessPoint(ap, batch, os, accessPointDataMap);
            }
        }

        if (batch.size() > 0) {
            processBatch(batch, os, accessPointDataMap);
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
    private boolean readAP(ApAccessPoint ap) {
        // set register type relation
        ApType rt = context.getStaticData().getApTypeById(ap.getApTypeId());
        ap.setApType(rt);

        // check party AP
        if (rt.getPartyType() != null) {
            context.addPartyAPId(ap.getAccessPointId());
            return false;
        }

        // check exported (can occur because of hierarchy)
        if (!exportedAPIds.add(ap.getAccessPointId())) {
            return false;
        }

        return true;
    }

    private void addAccessPoint(ApAccessPoint ap, List<ApAccessPoint> batch, AccessPointsOutputStream os, Map<Integer, ApAccessPointData> accessPointDataMap) {
        variantNameLoader.addRequest(ap.getAccessPointId(), new VariantNameDispatcher(ap));
        ApAccessPointData pointData = accessPointDataMap.get(ap.getAccessPointId());
        if (pointData != null && pointData.getExternalSystem() != null) {
            externalSystemLoader.addRequest(pointData.getExternalSystem().getExternalSystemId(), new ExternalSystemDispatcher(ap, pointData));
        }

        batch.add(ap);

        if (batch.size() >= context.getBatchSize()) {
            processBatch(batch, os, accessPointDataMap);
        }
    }

    private void processBatch(List<ApAccessPoint> batch, AccessPointsOutputStream os,
                              Map<Integer, ApAccessPointData> accessPointDataMap) {
        variantNameLoader.flush();
        externalSystemLoader.flush();
        batch.forEach(apAccessPoint ->
                os.addAccessPoint(apAccessPoint, accessPointDataMap.get(apAccessPoint.getAccessPointId())));
        batch.clear();
    }
}
