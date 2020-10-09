package cz.tacr.elza.dataexchange.output.aps;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.service.UserService;

public class ApInfoLoader extends AbstractEntityLoader<ApInfo, ApState> {

    private final Set<Integer> checkedScopeIds = new HashSet<>();

    private final ExternalIdLoader externalIdLoader;

    private final PartLoader partLoader;

    private final ItemLoader itemLoader;

    private final ExportContext context;

    private final UserService userService;

    private final boolean globalScopePermission;

    public ApInfoLoader(ExportContext context, EntityManager em, UserService userService) {
        super(ApState.class, ApState.FIELD_ACCESS_POINT_ID, em, context.getBatchSize());
        this.externalIdLoader = new ExternalIdLoader(em, batchSize);
        this.partLoader = new PartLoader(context,em,batchSize);
        this.itemLoader = new ItemLoader(context,em,batchSize);
        this.context = context;
        this.userService = userService;
        this.globalScopePermission = userService.hasPermission(Permission.AP_SCOPE_RD_ALL);
    }

    @Override
    public void flush() {
        super.flush();
        externalIdLoader.flush();
        partLoader.flush();
        itemLoader.flush();
    }

    @Override
    protected void buildExtendedQuery(Root<? extends ApState> baseEntity, CriteriaBuilder cb) {
        baseEntity.fetch(ApState.FIELD_ACCESS_POINT);
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ApState> root, CriteriaBuilder cb) {
        //SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s.accessPoint
        Subquery<Integer> subquery = cq.subquery(Integer.class);
        //CriteriaQuery<Integer> subquery = cb.createQuery(Integer.class);
        Root<ApState> sr = subquery.from(ApState.class);
        subquery.select(cb.max(sr.get(ApState.FIELD_CREATE_CHANGE_ID)))
                .where(cb.equal(sr.get(ApState.FIELD_ACCESS_POINT_ID), root.get(ApState.FIELD_ACCESS_POINT_ID)));

        return root.get(ApState.FIELD_CREATE_CHANGE_ID).in(subquery);
        //return root.get(ApState.FIELD_DELETE_CHANGE_ID).isNull();
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

        ExternalIdDispatcher eidd = new ExternalIdDispatcher(result, dispatcher, context.getStaticData());
        externalIdLoader.addRequest(accessPointId, eidd);

        PartDispatcher pd = new PartDispatcher(result, dispatcher, context.getStaticData());
        partLoader.addRequest(accessPointId, pd);



    }
}
