package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;
import javax.persistence.criteria.FetchParent;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrStructureItem;

public class StructItemLoader extends AbstractEntityLoader<ArrStructureItem> {

    public StructItemLoader(EntityManager em, int batchSize) {
        super(ArrStructureItem.class, ArrStructureItem.STRUCT_DATA_FK, em, batchSize);
    }

    @Override
    protected void setEntityFetch(FetchParent<?, ?> baseEntity) {
        baseEntity.fetch(ArrStructureItem.DATA);
    }
}
