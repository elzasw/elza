package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ParPartyInfo;


/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepository extends ElzaJpaRepository<ParParty, Integer>, PartyRepositoryCustom {

    /**
     * @param accessPointId id záznamu rejtříku
     * @return záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT ap FROM par_party ap JOIN ap.accessPoint r WHERE r.accessPointId = ?1")
    ParParty findParPartyByAccessPointId(Integer accessPointId);

    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param accessPoints seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT party FROM par_party party WHERE party.accessPoint IN (?1)")
    List<ParParty> findParPartyByAccessPoints(Collection<ApAccessPoint> accessPoints);

    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param accessPoints seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT party.partyId, r.accessPointId FROM par_party party JOIN party.accessPoint r WHERE r IN (?1)")
    List<Object[]> findAccessPointIdAndPartyIdByAccessPoints(Collection<ApAccessPoint> accessPoints);


    /**
     * Najde seznam tvůrců osoby podle vytvořené osoby.
     *
     * @param party vytvořená osoba
     * @return seznam tvůrců
     */
    @Query("SELECT c.creatorParty FROM par_creator c WHERE c.party = ?1 ORDER BY c.creatorId")
    List<ParParty> findCreatorsByParty(ParParty party);

    List<ParPartyInfo> findInfoByAccessPointIdIn(Collection<Integer> apIds);

    @Query("SELECT ap FROM par_party ap " +
            "LEFT JOIN FETCH ap.preferredName pn " +
            "LEFT JOIN FETCH pn.validFrom vf " +
            "LEFT JOIN FETCH vf.calendarType vfct " +
            "LEFT JOIN FETCH pn.validTo vt " +
            "LEFT JOIN FETCH vt.calendarType vtct " +
            "JOIN FETCH ap.partyType pt " +
            "JOIN FETCH ap.accessPoint rec " +
            "WHERE ap.partyId IN :ids")
    List<ParParty> findAllFetch(@Param("ids") Iterable<Integer> ids);

    @Query("SELECT distinct i.itemId" +
            " FROM arr_item i" +
            " JOIN arr_data_party_ref pr ON (pr.dataId = i.dataId)" +
            " WHERE pr.party.partyId = ?1")
    List<Integer> findItemIdByParty(Integer partyId);
}
