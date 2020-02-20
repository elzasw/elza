package cz.tacr.elza.dataexchange.output.aps;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.service.UserService;

public class ApInfoLoader extends AbstractEntityLoader<ApInfoImpl, ApAccessPoint> {

    private final Set<Integer> checkedScopeIds = new HashSet<>();

    private final NameLoader nameLoader;

    private final DescriptionLoader descriptionLoader;

    private final ExternalIdLoader externalIdLoader;

    private final ExportContext context;

    private final UserService userService;

    private final boolean globalScopePermission;

    public ApInfoLoader(ExportContext context, EntityManager em, UserService userService) {
        super(ApAccessPoint.class, ApAccessPoint.FIELD_ACCESS_POINT_ID, em, context.getBatchSize());
        this.nameLoader = new NameLoader(em, batchSize);
        this.descriptionLoader = new DescriptionLoader(em, batchSize);
        this.externalIdLoader = new ExternalIdLoader(em, batchSize);
        this.context = context;
        this.userService = userService;
        this.globalScopePermission = userService.hasPermission(Permission.AP_SCOPE_RD_ALL);
    }

    @Override
    public void flush() {
        super.flush();
        nameLoader.flush();
        descriptionLoader.flush();
        externalIdLoader.flush();
    }

    @Override
    protected ApInfoImpl createResult(Object entity) {
        ApAccessPoint ap = (ApAccessPoint) entity;
        return new ApInfoImpl(ap);
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<ApInfoImpl> dispatcher, ApInfoImpl result) {
        ApAccessPoint ap = result.getAp();

        // check scope permissions
        if (!globalScopePermission && checkedScopeIds.add(ap.getScopeId())) {
            if (!userService.hasPermission(Permission.AP_SCOPE_RD, ap.getScopeId())) {
                throw Authorization.createAccessDeniedException(Permission.AP_SCOPE_RD);
            }
        }

        // we must ignore party AP (AP type initialized by dispatcher)
        ParPartyType partyType = ap.getApType().getPartyType();
        if (partyType != null) {
            context.addPartyApId(ap.getAccessPointId());
            result.setPartyAp(true);
            return;
        }

        NameDispatcher nd = new NameDispatcher(result, dispatcher, context.getStaticData());
        nameLoader.addRequest(ap.getAccessPointId(), nd);

        DescriptionDispatcher dd = new DescriptionDispatcher(result, dispatcher);
        descriptionLoader.addRequest(ap.getAccessPointId(), dd);

        ExternalIdDispatcher eidd = new ExternalIdDispatcher(result, dispatcher, context.getStaticData());
        externalIdLoader.addRequest(ap.getAccessPointId(), eidd);
    }
}
