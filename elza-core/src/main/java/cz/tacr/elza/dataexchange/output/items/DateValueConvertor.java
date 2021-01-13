package cz.tacr.elza.dataexchange.output.items;

import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemDate;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class DateValueConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataDate.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataDate dataValue = (ArrDataDate) data;

        DescriptionItemDate item = objectFactory.createDescriptionItemDate();

        Date localDate = dataValue.getDate();
        XMLGregorianCalendar xmlGregorianCalendar = XmlUtils.convertDate(localDate);
        item.setV(xmlGregorianCalendar);
        return item;
    }

}
