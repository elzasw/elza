package cz.tacr.elza.drools.model;

import java.util.List;

import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.service.StructObjValueService;

/**
 * Objekt strukt. datového typu atributu.
 *
 * @since 16.11.2017
 */
public class Structured {

    private final ArrStructuredObject structObj;

    private final StructObjValueService structObjService;

    private List<StructObjItem> items;

    public Structured(final ArrStructuredObject structObj, final StructObjValueService structObjService) {
		this.structObj = structObj;
		this.structObjService = structObjService;
	}

	public String getValue() {
        return structObj.getValue();
    }

	public List<StructObjItem> getItems() {
		if(items==null) {
			// read description items (if needed) from DB
			List<ArrStructuredItem> dbItems = structObjService.findByStructuredObjectAndDeleteChangeIsNullFetchData(structObj);
			items = ModelFactory.createStructuredItems(dbItems);
		}
		return items;
	}
}
