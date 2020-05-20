package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrSobjVrequest;
import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * Repository for ArrSobjVrequest
 * 
 *
 */
@Repository
public interface SobjVrequestRepository extends JpaRepository<ArrSobjVrequest, Integer> {

    int deleteByStructuredObjectFundId(Integer fundId);

    @Modifying
    @Query("DELETE FROM " + ArrSobjVrequest.TABLE_NAME + " v WHERE v."
            + ArrSobjVrequest.FIELD_STRUCTURED_OBJECT_ID
            + " IN ("
            + "SELECT sd." + ArrStructuredObject.FIELD_STRUCTURED_OBJECT_ID + " FROM " + ArrStructuredObject.TABLE_NAME
            + " sd WHERE sd." + ArrStructuredObject.FIELD_STATE
            + " = 'TEMP'"
            + ")")
    void deleteByStructuredObjectStateTemp();

}
