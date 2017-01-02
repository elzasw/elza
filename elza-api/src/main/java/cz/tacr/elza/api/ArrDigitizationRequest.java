package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Dotaz pro externí systémy - Vytvoření digitalizátu.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrDigitizationRequest<DF extends ArrDigitizationFrontdesk> extends Serializable {

    DF getDigitizationFrontdesk();

    void setDigitizationFrontdesk(DF digitizationFrontdesk);

    String getDescription();

    void setDescription(String description);

}
