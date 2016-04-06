package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataUnitdate<CT extends ArrCalendarType> extends Serializable, IUnitdate<CT> {

    /**
     * @return vrací datum "od" v iso formátu
     */
    @Override
    String getValueFrom();


    /**
     * @param valueFrom datum "od" v iso formátu
     */
    @Override
    void setValueFrom(String valueFrom);


    /**
     * @return je datum "od" přibližný?
     */
    @Override
    Boolean getValueFromEstimated();


    /**
     * @param valueFromEstimated je datum "od" přibližný?
     */
    @Override
    void setValueFromEstimated(Boolean valueFromEstimated);


    /**
     * @return vrací datum "do" v iso formátu
     */
    @Override
    String getValueTo();


    /**
     * @param valueTo datum "do" v iso formátu
     */
    @Override
    void setValueTo(String valueTo);


    /**
     * @return je datum "do přibližný?
     */
    @Override
    Boolean getValueToEstimated();


    /**
     * @param valueToEstimated je datum "do přibližný?
     */
    @Override
    void setValueToEstimated(Boolean valueToEstimated);

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
    @Override
    String getFormat();


    /**
     * @param format formát, jakým způsobem se má zobrazovat výstup
     */
    @Override
    void setFormat(String format);

    /**
     * @return počet sekund v normalizačním kalendáři - od
     */
    Long getNormalizedFrom();

    /**
     * @param normalizedFrom počet sekund v normalizačním kalendáři - od
     */
    void setNormalizedFrom(Long normalizedFrom);

    /**
     * @return počet sekund v normalizačním kalendáři - do
     */
    Long getNormalizedTo();

    /**
     * @param normalizedFrom počet sekund v normalizačním kalendáři - do
     */
    void setNormalizedTo(Long normalizedFrom);

}
