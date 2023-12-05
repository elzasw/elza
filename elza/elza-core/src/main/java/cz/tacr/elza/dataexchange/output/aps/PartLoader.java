package cz.tacr.elza.dataexchange.output.aps;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ApPart;

public class PartLoader extends AbstractEntityLoader<ApPart, ApPart> {

    //private final ItemLoader itemLoader;

    private final ExportContext context;

    public PartLoader(ExportContext context, EntityManager em, int batchSize) {
        super(ApPart.class, ApPart.ACCESS_POINT_ID, em, batchSize);
       // this.itemLoader = new ItemLoader(em, batchSize);
        this.context = context;
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ApPart> root, CriteriaBuilder cb) {
        return root.get(ApPart.DELETE_CHANGE_ID).isNull();
    }

    /*@Override
    public void flush() {
        super.flush();
        itemLoader.flush();
    }*/

   /* @Override
    protected void onBatchEntryLoad(LoadDispatcher<ApInfo> dispatcher, ApInfo result) {
        ApInfo apInfo = ((ApInfoDispatcher) dispatcher).getApInfo();
        for (ApPart part : apInfo.getParts()) {
            ItemDispatcher id = new ItemDispatcher(result, dispatcher, context.getStaticData());
            itemLoader.addRequest(part.getPartId(), id);
        }
    }*/
}
