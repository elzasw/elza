package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.schema.v2.DescriptionItemBit;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

public class DescriptionItemBitImpl extends DescriptionItemBit {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if(dataType != DataType.BIT) {
            throw new DEImportException("Unsupported data type:" +dataType);
        }

        //TODO: zpracovat případnou hodnotu value
        ArrDataBit data = new ArrDataBit();
        data.setValue(isValue());
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data);
    }

}
