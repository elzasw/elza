package cz.tacr.elza.controller.vo.ap.item;

import java.util.Objects;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class ApItemUnitdateVO extends ApItemVO {

    /**
     * Hodnota UnitDate
     */
    private String value;

    public ApItemUnitdateVO() {
    }

    /**
     * @param languageTag
     *            language of the text of the value (the UI language of the request); null for the
     *            default language
     */
    public ApItemUnitdateVO(final AccessPointItem item, final String languageTag) {
        super(item);
        ArrDataUnitdate data = HibernateUtils.unproxy(item.getData());
        if (data != null) {
            value = UnitDateConverter.convertToString(data, languageTag);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrDataUnitdate createDataEntity(EntityManager em) {
        ArrDataUnitdate data = ArrDataUnitdate.valueOf(value);
        return data;
    }

    /**
     * Compares the stored form of the dates, not their text: the text of this VO is in the language
     * of the request, the stored value has none.
     */
    @Override
    public boolean equalsValue(AccessPointItem item) {
        if (!equalsBase(item)) {
            return false;
        }
        ArrDataUnitdate data = HibernateUtils.unproxy(item.getData());
        if (data == null || value == null) {
            return data == null && value == null;
        }
        ArrDataUnitdate parsed;
        try {
            parsed = UnitDateConverter.convertToUnitDate(value, new ArrDataUnitdate());
        } catch (RuntimeException e) {
            return false;
        }
        return Objects.equals(parsed.getFormat(), data.getFormat())
                && Objects.equals(parsed.getValueFrom(), trim(data.getValueFrom()))
                && Objects.equals(parsed.getValueTo(), trim(data.getValueTo()))
                && Objects.equals(parsed.getValueFromEstimated(), data.getValueFromEstimated())
                && Objects.equals(parsed.getValueToEstimated(), data.getValueToEstimated());
    }

    /** Values read from the database may carry trailing spaces. */
    private static String trim(final String value) {
        return value != null ? value.trim() : null;
    }
}
