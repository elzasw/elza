package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ParParty;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DataPartyRefRepository extends JpaRepository<ArrDataPartyRef, Integer>, DataPartyRefRepositoryCustom {

    /**
     * Najde počet záznamů podle par party.
     * @param party
     * @return
     */
    @Query("SELECT count(*) FROM arr_data_party_ref i WHERE i.party = ?1")
    Long getCountByParty(ParParty party);

    @Query("SELECT i FROM arr_data_party_ref i WHERE i.party = ?1")
	List<ArrDataPartyRef> findByParty(ParParty party);
}
