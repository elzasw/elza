package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrSearchWork;

@Repository
public interface SearchWorkRepository extends JpaRepository<ArrSearchWork, Integer> {
}
