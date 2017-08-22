package cz.tacr.elza.deimport.sections.items;

import java.time.LocalDateTime;
import java.time.ZoneId;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.TimeIntervalConvertor;
import cz.tacr.elza.deimport.processor.TimeIntervalConvertor.TimeIntervalConversionResult;
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
    protected ArrData createData(ImportContext context, RuleSystemItemType itemType) {
        ArrDataUnitdate data = new ArrDataUnitdate();

        // time interval conversion
        TimeIntervalConversionResult convResult = new TimeIntervalConvertor(context.getDatatypeFactory()).convert(getD());
        data.setCalendarType(convResult.getCalendarType().getEntity());
        data.setFormat(convResult.getFormat());
        data.setValueFrom(convResult.getFormattedFrom());
        data.setValueTo(convResult.getFormattedTo());
        data.setValueFromEstimated(convResult.isFromEst());
        data.setValueToEstimated(convResult.isToEst());

        // normalization of time interval
        LocalDateTime fromLDT = LocalDateTime.ofInstant(convResult.getFrom().toInstant(), ZoneId.systemDefault()); // TODO: time zone
        LocalDateTime toLDT = LocalDateTime.ofInstant(convResult.getTo().toInstant(), ZoneId.systemDefault()); // TODO: time zone
        long normFromSec = CalendarConverter.toSeconds(convResult.getCalendarType(), fromLDT);
        long normToSec = CalendarConverter.toSeconds(convResult.getCalendarType(), toLDT);
        data.setNormalizedFrom(normFromSec);
        data.setNormalizedTo(normToSec);

        return data;
    }
}
