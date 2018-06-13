package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ParPartyInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepository extends ElzaJpaRepository<ParParty, Integer>, PartyRepositoryCustom {

    /**
     * @param recordId id záznamu rejtříku
     * @return záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT ap FROM par_party ap JOIN ap.record r WHERE r.accessPointId = ?1")
    List<ParParty> findParPartyByRecordId(Integer recordId);


    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param records seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT party FROM par_party party WHERE party.record IN (?1)")
    List<ParParty> findParPartyByRecords(Collection<ApAccessPoint> records);

    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param records seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT party.partyId, r.accessPointId FROM par_party party JOIN party.record r WHERE r IN (?1)")
    List<Object[]> findRecordIdAndPartyIdByRecords(Collection<ApAccessPoint> records);


    /**
     * Najde seznam tvůrců osoby podle vytvořené osoby.
     *
     * @param party vytvořená osoba
     * @return seznam tvůrců
     */
    @Query("SELECT c.creatorParty FROM par_creator c WHERE c.party = ?1 ORDER BY c.creatorId")
    List<ParParty> findCreatorsByParty(ParParty party);

    List<ParPartyInfo> findInfoByRecordAccessPointIdIn(Collection<Integer> apIds);

    @Query("SELECT ap FROM par_party ap " +
            "LEFT JOIN FETCH ap.preferredName pn " +
            "JOIN FETCH pn.nameFormType nft " +
            "LEFT JOIN FETCH pn.validFrom vf " +
            "LEFT JOIN FETCH vf.calendarType vfct " +
            "LEFT JOIN FETCH pn.validTo vt " +
            "LEFT JOIN FETCH vt.calendarType vtct " +
            "JOIN FETCH ap.partyType pt " +
            "JOIN FETCH ap.record rec " +
            "WHERE ap.partyId IN :ids")
    List<ParParty> findAllFetch(@Param("ids") Iterable<Integer> ids);
}
