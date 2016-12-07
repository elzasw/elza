package cz.tacr.elza.domain;

import javax.persistence.Column;
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
public class ArrDaoLinkRequest extends ArrRequest implements cz.tacr.elza.api.ArrDaoLinkRequest<ArrDigitalRepository, ArrNode, ArrDao> {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

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
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
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
