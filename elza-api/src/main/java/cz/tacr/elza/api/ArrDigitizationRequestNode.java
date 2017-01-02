package cz.tacr.elza.api;

/**
 * Vazební entita mezi nodem a požadavkem na digitalizaci.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrDigitizationRequestNode<DR extends ArrDigitizationRequest, N extends ArrNode> {


    Integer getDigitizationRequestNodeId();

    void setDigitizationRequestNodeId(Integer digitizationRequestNodeId);

    DR getDigitizationRequest();

    void setDigitizationRequest(DR digitizationRequest);

    N getNode();

    void setNode(N node);
}
