package cz.tacr.elza.repository;

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

    @Query("select dr from da_dao_relation dr where dr.parentDao = :daDao and dr.deleteChange = null")
    List<DaDaoRelation> findByParentDaoAndDeleteChangeIsNull(@Param("daDao") DaDao daDao);
}
