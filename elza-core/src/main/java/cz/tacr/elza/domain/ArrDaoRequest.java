package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_request")
@Table
@DiscriminatorValue(value= ArrRequest.ClassType.Values.DAO)
public class ArrDaoRequest extends ArrRequest implements cz.tacr.elza.api.ArrDaoRequest<ArrDigitizationFrontdesk> {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitizationFrontdesk.class)
    @JoinColumn(name = "digitizationFrontdeskId", nullable = false)
    private ArrDigitizationFrontdesk digitizationFrontdesk;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Type type;

    @Column(length = StringLength.LENGTH_1000)
    private String description;

    @Override
    public ArrDigitizationFrontdesk getDigitizationFrontdesk() {
        return digitizationFrontdesk;
    }

    @Override
    public void setDigitizationFrontdesk(final ArrDigitizationFrontdesk digitizationFrontdesk) {
        this.digitizationFrontdesk = digitizationFrontdesk;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(final Type type) {
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }
}
