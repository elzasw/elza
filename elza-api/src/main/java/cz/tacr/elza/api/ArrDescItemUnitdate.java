package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemUnitdate<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, CT extends ArrCalendarType> extends ArrDescItem<FC, RT, RS, N> {

    String getValueFrom();

    void setValueFrom(final String valueFrom);

    Boolean getValueFromEstimated();

    void setValueFromEstimated(final Boolean valueFromEstimated);

    String getValueTo();

    void setValueTo(final String valueTo);

    Boolean getValueToEstimated();

    void setValueToEstimated(final Boolean valueToEstimated);

    CT getCalendarType();

    void setCalendarType(CT calendarType);
}
