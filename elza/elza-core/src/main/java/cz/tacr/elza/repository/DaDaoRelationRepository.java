package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoRelationRepository extends JpaRepository<DaDaoRelation, Integer> {

    List<DaDaoRelation> findByDaoInAndDeleteChangeIsNull(List<DaDao> daDaoList);

    @Query("select dr from da_dao_relation dr where dr.parentDao = :daDao and dr.deleteChange is null")
    List<DaDaoRelation> findByParentDaoAndDeleteChangeIsNull(@Param("daDao") DaDao daDao);

    @Query("select dr from da_dao_relation dr join fetch dr.parentDao pd join fetch dr.dao d where (pd.aip in :aips) and dr.deleteChange is null")
    List<DaDaoRelation> findByAipsAndDeleteChangeIsNull(@Param("aips") List<DaAip> aips);

    @Query("select distinct pd from da_dao_relation dr join dr.parentDao pd where (pd.aip in :aips) and dr.deleteChange is null and " +
            "not exists (select d from da_dao_relation d where d.dao = pd)")
    List<DaDao> findParentDaosByAipsAndDeleteChangeIsNull(@Param("aips") List<DaAip> aips);
}
