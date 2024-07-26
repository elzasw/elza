package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaLevelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DaLevelViewRepository extends JpaRepository<DaLevelView, Integer> {

    DaLevelView findByParentLevelViewAndLabelAndDeleteChangeIsNull(DaLevelView parentLevelView, String label);

    @Query("select lv from da_level_view lv where lv.deleteChange is null and not exists (select d.levelView from da_dao d where d.levelView = lv and d.deleteChange is null)")
    List<DaLevelView> findDisconnectedLevelViews();
}
