package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApPartRepository extends JpaRepository<ApPart, Integer> {

}
