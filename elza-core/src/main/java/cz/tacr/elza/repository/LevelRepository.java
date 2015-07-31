package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.Level;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<Level, Integer> {

    @Query(value = "SELECT max(l.treeId) FROM arr_level l")
    Integer findMaxTreeId();

    List<Level> findByLevelId(List<Integer> levelIds);
}
