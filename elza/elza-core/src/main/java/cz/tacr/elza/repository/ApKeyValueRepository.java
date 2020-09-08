package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApKeyValueRepository extends JpaRepository<ApKeyValue, Integer> {

    ApKeyValue findByKeyTypeAndValueAndScope(String keyType, String value, ApScope scope);
}
