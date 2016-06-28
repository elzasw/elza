package cz.tacr.elza.api;

/**
 * Doporučení hromadné akce pro typ výstupu.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
public interface RulActionRecommended<A extends RulAction, OT extends RulOutputType> {

    /**
     * @return identifikátor entity
     */
    Integer getActionRecommendedId();

    /**
     * @param actionRecommendedId identifikátor entity
     */
    void setActionRecommendedId(Integer actionRecommendedId);

    /**
     * @return hromadná akce
     */
    A getAction();

    /**
     * @param action hromadná akce
     */
    void setAction(A action);

    /**
     * @return typ výstupu
     */
    OT getOutputType();

    /**
     * @param outputType typ výstupu
     */
    void setOutputType(OT outputType);
}
