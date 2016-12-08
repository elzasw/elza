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
 * Implementace {@link cz.tacr.elza.api.ArrDigitizationRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_link_request")
@Table
@DiscriminatorValue(value="DAO_LINK")
public class ArrDaoLinkRequest extends ArrRequest implements cz.tacr.elza.api.ArrDaoLinkRequest<ArrDigitalRepository, ArrDao> {

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

    @Override
    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    @Override
    public void setDigitalRepository(final ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    @Override
    public ArrDao getDao() {
        return dao;
    }

    @Override
    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }

    @Override
    public String getDidCode() {
        return didCode;
    }

    @Override
    public void setDidCode(final String didCode) {
        this.didCode = didCode;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(final Type type) {
        this.type = type;
    }
}
