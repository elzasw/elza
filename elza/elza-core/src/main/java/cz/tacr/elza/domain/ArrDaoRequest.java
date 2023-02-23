package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Dotaz pro externí systémy - DAO / delimitace nebo skartace.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_request")
@Table
@DiscriminatorValue(value= ArrRequest.ClassType.Values.DAO)
public class ArrDaoRequest extends ArrRequest {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Type type;

    @Column(length = StringLength.LENGTH_1000)
    private String description;

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(final ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public enum Type {

        /**
         * Skartace.
         */
        DESTRUCTION,

        /**
         * Delimitace.
         */
        TRANSFER,

        /**
         * Synchronizace digitalizátů
         */
        SYNC
    }
}
