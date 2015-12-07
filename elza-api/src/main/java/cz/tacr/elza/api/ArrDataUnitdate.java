package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataUnitdate<CT extends ArrCalendarType> extends Serializable {

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
     * @return identifikátor typu kalendáře
     */
    Integer getCalendarTypeId();


    /**
     * @param calendarTypeId identifikátor typu kalendáře
     */
    void setCalendarTypeId(Integer calendarTypeId);


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

}
