package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.common.timeinterval.TimeInterval;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;

public class DescriptionItemUnitDateImpl extends DescriptionItemUnitDate {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.UNITDATE) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        ArrDataUnitdate data = new ArrDataUnitdate();
        data.setDataType(dataType.getEntity());
        
        // time interval conversion
        TimeInterval it = TimeInterval.create(getD());
        data.setCalendarType(it.getCalendarType().getEntity());
        data.setFormat(it.getFormat());
        data.setValueFrom(it.getFormattedFrom());
        data.setValueTo(it.getFormattedTo());
        data.setValueFromEstimated(it.isFromEst());
        data.setValueToEstimated(it.isToEst());

        // normalization of time interval
        long normFromSec = CalendarConverter.toSeconds(it.getCalendarType(), it.getFrom());
        long normToSec = CalendarConverter.toSeconds(it.getCalendarType(), it.getTo());
        data.setNormalizedFrom(normFromSec);
        data.setNormalizedTo(normToSec);

        return new ImportableItemData(data);
    }
}
