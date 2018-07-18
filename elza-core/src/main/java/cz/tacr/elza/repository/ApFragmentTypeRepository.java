package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApFragmentTypeRepository extends JpaRepository<ApFragmentType, Integer> {

}
