package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUriRef;

public class UriRefItemConvertor extends AbstractItemConvertor {


    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if(itemType.getDataType() != DataType.URI_REF) {
            return null;
        }
        ArrDataUriRef data = (ArrDataUriRef) item.getData();
        ArrNode linkedNode = data.getArrNode();
        Node node = null;
        if(linkedNode!=null) {
        	node = context.getNode(data.getArrNode());
        }

        return new ItemUriRef(data.getSchema(), data.getValue(), data.getDescription(), node);
    }
}
