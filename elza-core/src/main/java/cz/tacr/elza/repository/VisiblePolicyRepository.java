package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UIVisiblePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface VisiblePolicyRepository extends JpaRepository<UIVisiblePolicy, Integer> {

}
