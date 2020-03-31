package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ArrDataUriRef;

import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author Tomáš Gotzy
 * @since 28.02.2002 *
 */
public interface DataUriRefRepository extends JpaRepository<ArrDataUriRef, Integer> {

    @Modifying
    @Query("UPDATE ArrDataUriRef ur SET ur.nodeId=null WHERE ur.nodeId in ?1")
    void updateByNodesIdIn(Collection<Integer> nodeIds);

    @Query("SELECT ur FROM ArrDataUriRef ur WHERE ur.value = ?1")
    List<ArrDataUriRef> findAllByNodeUUID(String value);





}
