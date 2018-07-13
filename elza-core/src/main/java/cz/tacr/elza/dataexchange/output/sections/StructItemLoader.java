package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;
import javax.persistence.criteria.FetchParent;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrStructuredItem;

public class StructItemLoader extends AbstractEntityLoader<ArrStructuredItem> {

    public StructItemLoader(EntityManager em, int batchSize) {
        super(ArrStructuredItem.class, ArrStructuredItem.STRUCT_OBJ_FK, em, batchSize);
    }

    @Override
    protected void setQueryFetch(FetchParent<?, ?> root) {
        root.fetch(ArrStructuredItem.DATA);
    }
}
