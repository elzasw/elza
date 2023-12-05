package cz.tacr.elza.print.item.convertors;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemType;

public class DateItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(Item item, ItemType itemType) {
        if (itemType.getDataType() != DataType.DATE) {
            return null;
        }

        //
        ArrData data = HibernateUtils.unproxy(item.getData());
        if (data == null) {
            return null;
        }
        ArrDataDate dataDate = (ArrDataDate) data;

        // get locale
        Locale locale = this.context.getLocale();

        Date date = dataDate.getDate();

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);

        LocalDate locDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String value = locDate.format(formatter);

        return new ItemString(value);
    }

}
