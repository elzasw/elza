package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApScope;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApKeyValueRepository extends JpaRepository<ApKeyValue, Integer> {

    ApKeyValue findByKeyTypeAndValueAndScope(String keyType, String value, ApScope scope);

    @Query("SELECT k.keyValueId FROM ApPart p JOIN p.keyValue k WHERE p.accessPointId IN :apIds")
    List<Integer> findAllIdByAccessPointIdIn(@Param("apIds") Collection<Integer> apIds);
}
