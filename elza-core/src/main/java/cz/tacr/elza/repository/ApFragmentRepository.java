package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApFragmentRepository extends JpaRepository<ApFragment, Integer> {

}
