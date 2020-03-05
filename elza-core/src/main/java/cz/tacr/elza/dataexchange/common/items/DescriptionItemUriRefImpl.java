package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;

public class DescriptionItemUriRefImpl extends DescriptionItemUriRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if(dataType != DataType.URI_REF) {
            throw new DEImportException("Unsupported data type: " + dataType);
        }

        //TODO: neco chybi

       /* ArrDataUriRef data = new ArrDataUriRef();
        data.setArrNode();*/

        throw new DEImportException("Not implemented for : " + dataType);
    }


}
