package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.dataexchange.common.CalendarTypeConvertor;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.ObjectFactory;
import cz.tacr.elza.schema.v2.TimeInterval;

public class UnitDateValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemUnitDate convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataUnitdate.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataUnitdate unitdate = (ArrDataUnitdate) data;

        TimeInterval interval = new TimeInterval();
        interval.setF(unitdate.getValueFrom());
        interval.setTo(unitdate.getValueTo());
        interval.setFe(unitdate.getValueFromEstimated());
        interval.setToe(unitdate.getValueToEstimated());
        interval.setFmt(unitdate.getFormat());
        CalendarType calendarType = CalendarType.fromId(unitdate.getCalendarType().getCalendarTypeId());
        interval.setCt(CalendarTypeConvertor.convert(calendarType));

        DescriptionItemUnitDate item = objectFactory.createDescriptionItemUnitDate();
        item.setD(interval);
        return item;
    }
}
