package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Dotaz pro externí systémy - připojení / odpojení DAO k JP.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_link_request")
@Table
@DiscriminatorValue(value = ArrRequest.ClassType.Values.DAO_LINK)
public class ArrDaoLinkRequest extends ArrRequest {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String didCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Type type;

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(final ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }

    public String getDidCode() {
        return didCode;
    }

    public void setDidCode(final String didCode) {
        this.didCode = didCode;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public enum Type {

        /**
         * Připojení k JP.
         */
        LINK,

        /**
         * Odpojení od JP.
         */
        UNLINK
    }
}
