package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ApAccessPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.projection.ParInstitutionInfo;

/**
 * Repository pro {@link ParInstitution}.
 *
 * @since 18.3.2016
 */
@Repository
public interface InstitutionRepository extends JpaRepository<ParInstitution, Integer> {

    ParInstitution findByInternalCode(String institutionInternalCode);

    //ParInstitution findByParty(ParParty parParty);

    ParInstitution findByAccessPoint(ApAccessPoint accessPoint);

    //List<ParInstitutionInfo> findInfoByPartyIdIn(Collection<Integer> partyIds);

    List<ParInstitutionInfo> findInfoByAccessPointIdIn(Collection<Integer> accessPointIds);

    @Query("SELECT i FROM par_institution i " +
            "JOIN FETCH i.accessPoint ap " +
            "LEFT JOIN FETCH ap.preferredPart prefPart ")
    List<ParInstitution> findAllWithFetch();

    @Modifying
    int deleteByInstitutionIdIn(Collection<Integer> partyIds);

    @Query("SELECT i FROM arr_fund f JOIN f.institution i JOIN FETCH i.institutionType t JOIN FETCH i.accessPoint ap WHERE f=?1")
    ParInstitution findByFundFetchTypeAndAccessPoint(ArrFund arrFund);

    @Query("SELECT i FROM par_institution i JOIN FETCH i.accessPoint ap WHERE ap.uuid=?1")
    ParInstitution findByAccessPointUUID(String uuid);

    @Query("SELECT distinct i FROM arr_fund f JOIN f.institution i JOIN FETCH i.accessPoint ap JOIN FETCH ap.preferredPart pref")
    List<ParInstitution> findListByFundFetchAccessPointFetchPreferredPart();


}
