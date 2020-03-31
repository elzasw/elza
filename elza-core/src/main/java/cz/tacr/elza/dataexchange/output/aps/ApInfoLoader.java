package cz.tacr.elza.dataexchange.output.aps;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.service.UserService;

public class ApInfoLoader extends AbstractEntityLoader<ApInfo, ApState> {

    private final Set<Integer> checkedScopeIds = new HashSet<>();

    private final NameLoader nameLoader;

    private final DescriptionLoader descriptionLoader;

    private final ExternalIdLoader externalIdLoader;

    private final ExportContext context;

    private final UserService userService;

    private final boolean globalScopePermission;

    public ApInfoLoader(ExportContext context, EntityManager em, UserService userService) {
        super(ApState.class, ApState.FIELD_ACCESS_POINT_ID, em, context.getBatchSize());
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
    protected void buildExtendedQuery(Root<? extends ApState> baseEntity, CriteriaBuilder cb) {
        baseEntity.fetch(ApState.FIELD_ACCESS_POINT);
    }

    @Override
    protected Predicate createQueryCondition(Path<? extends ApState> root, CriteriaBuilder cb) {
        return root.get(ApState.FIELD_DELETE_CHANGE_ID).isNull();
    }

    @Override
    protected ApInfo createResult(Object entity) {
        ApState apState = (ApState) entity;
        Validate.isTrue(HibernateUtils.isInitialized(apState.getAccessPoint()));
        return new ApInfo(apState);
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<ApInfo> dispatcher, ApInfo result) {

        ApState apState = result.getApState();
        Integer accessPointId = apState.getAccessPointId();

        // check scope permissions
        if (!globalScopePermission && checkedScopeIds.add(apState.getScopeId())) {
            if (!userService.hasPermission(Permission.AP_SCOPE_RD, apState.getScopeId())) {
                throw Authorization.createAccessDeniedException(Permission.AP_SCOPE_RD);
            }
        }

        // we must ignore party AP (AP type initialized by dispatcher)
        ParPartyType partyType = apState.getApType().getPartyType();
        if (partyType != null) {
            context.addPartyApId(accessPointId);
            result.setPartyAp(true);
            return;
        }

        NameDispatcher nd = new NameDispatcher(result, dispatcher, context.getStaticData());
        nameLoader.addRequest(accessPointId, nd);

        DescriptionDispatcher dd = new DescriptionDispatcher(result, dispatcher);
        descriptionLoader.addRequest(accessPointId, dd);

        ExternalIdDispatcher eidd = new ExternalIdDispatcher(result, dispatcher, context.getStaticData());
        externalIdLoader.addRequest(accessPointId, eidd);
    }
}
