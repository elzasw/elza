/**
 * 
 */
package cz.tacr.elza.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ParPartyName;

/**
 * @author vavrejn
 *
 */
@Component
public class PartyNameRepositoryImpl implements PartyNameCustomRepository {


    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void unsetAllParty() {
        entityManager.createQuery("update par_party_name set " + ParPartyName.FIELD_PARTY + " = null").executeUpdate();
    }
}
