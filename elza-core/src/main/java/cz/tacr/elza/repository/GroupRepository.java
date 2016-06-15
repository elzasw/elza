package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro {@link UsrGroup}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Repository
public interface GroupRepository extends JpaRepository<UsrGroup, Integer>, GroupRepositoryCustom {
}
