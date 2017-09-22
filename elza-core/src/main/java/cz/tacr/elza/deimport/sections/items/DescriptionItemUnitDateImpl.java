package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.timeinterval.TimeInterval;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;

public class DescriptionItemUnitDateImpl extends DescriptionItemUnitDate {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.UNITDATE;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        ArrDataUnitdate data = new ArrDataUnitdate();

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

        return data;
    }
}
