package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemUnitdate<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec,
        N extends ArrNode, CT extends ArrCalendarType> extends ArrDescItem<FC, RT, RS, N> {

    /**
     * @return vrací datum "od" v iso formátu
     */
    String getValueFrom();


    /**
     * @param valueFrom datum "od" v iso formátu
     */
    void setValueFrom(String valueFrom);


    /**
     * @return je datum "od" přibližný?
     */
    Boolean getValueFromEstimated();


    /**
     * @param valueFromEstimated je datum "od" přibližný?
     */
    void setValueFromEstimated(Boolean valueFromEstimated);


    /**
     * @return vrací datum "do" v iso formátu
     */
    String getValueTo();


    /**
     * @param valueTo datum "do" v iso formátu
     */
    void setValueTo(String valueTo);


    /**
     * @return je datum "do přibližný?
     */
    Boolean getValueToEstimated();


    /**
     * @param valueToEstimated je datum "do přibližný?
     */
    void setValueToEstimated(Boolean valueToEstimated);


    /**
     * @return typ kalendáře
     */
    CT getCalendarType();


    /**
     * @param calendarType typ kalendáře
     */
    void setCalendarType(CT calendarType);


    /**
     * Možnosti hodnot formátu:
     * - století: C
     * - rok: Y
     * - rok/měsíc: YM
     * - datum: D
     * - datum a čas: DT
     *
     * Formát může být zadán:
     * - jako jedna hodnota (např. Y)
     * - jako interval (např. Y-Y)
     * - jako polointernval (např. Y-)
     *
     * @return formát, jakým způsobem se má zobrazovat výstup
     */
    String getFormat();


    /**
     * @param format formát, jakým způsobem se má zobrazovat výstup
     */
    void setFormat(String format);


    /**
     * @param format přidání řetězce k formátu
     */
    void formatAppend(String format);
}
