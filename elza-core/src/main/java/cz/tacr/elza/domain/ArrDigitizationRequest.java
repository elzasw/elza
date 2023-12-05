package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Dotaz pro externí systémy - Vytvoření digitalizátu.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_digitization_request")
@Table
@DiscriminatorValue(ArrRequest.ClassType.Values.DIGITIZATION)
public class ArrDigitizationRequest extends ArrRequest {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitizationFrontdesk.class)
    @JoinColumn(name = "digitizationFrontdeskId", nullable = false)
    private ArrDigitizationFrontdesk digitizationFrontdesk;

    @Column(length = StringLength.LENGTH_1000)
    private String description;

    public ArrDigitizationFrontdesk getDigitizationFrontdesk() {
        return digitizationFrontdesk;
    }

    public void setDigitizationFrontdesk(final ArrDigitizationFrontdesk digitizationFrontdesk) {
        this.digitizationFrontdesk = digitizationFrontdesk;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
