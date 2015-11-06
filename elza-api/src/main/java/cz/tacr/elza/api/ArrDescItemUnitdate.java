package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemUnitdate<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, CT extends ArrCalendarType> extends ArrDescItem<FC, RT, RS, N> {

    String getValueFrom();

    void setValueFrom(String valueFrom);

    Boolean getValueFromEstimated();

    void setValueFromEstimated(Boolean valueFromEstimated);

    String getValueTo();

    void setValueTo(String valueTo);

    Boolean getValueToEstimated();

    void setValueToEstimated(Boolean valueToEstimated);

    CT getCalendarType();

    void setCalendarType(CT calendarType);

    String getFormat();

    void setFormat(String format);

    void formatAppend(String format);
}
