package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApFragmentItemRepository extends JpaRepository<ApFragmentItem, Integer> {

}
