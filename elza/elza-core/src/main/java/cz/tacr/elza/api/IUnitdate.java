package cz.tacr.elza.api;

/**
 * Rozhraní pro datace.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 26.01.2016
 */
public interface IUnitdate {

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
