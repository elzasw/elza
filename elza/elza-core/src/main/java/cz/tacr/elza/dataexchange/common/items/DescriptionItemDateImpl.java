package cz.tacr.elza.dataexchange.common.items;

import java.time.LocalDate;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.schema.v2.DescriptionItemDate;

public class DescriptionItemDateImpl extends DescriptionItemDate {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.DATE) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        ArrDataDate data = new ArrDataDate();
        LocalDate locDate = XmlUtils.convertToLocalDate(v);
        data.setValue(locDate);
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data);
    }

}
