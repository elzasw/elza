package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepository extends JpaRepository<ParParty, Integer>, PartyRepositoryCustom {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT ap FROM par_party ap JOIN ap.record r WHERE r.recordId = ?1")
    List<ParParty> findParAbstractPartyByRecordId(Integer recordId);

}
