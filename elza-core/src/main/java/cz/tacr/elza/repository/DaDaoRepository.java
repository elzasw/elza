package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaLevelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoRepository extends JpaRepository<DaDao, Integer> {

    List<DaDao> findByAipAndDeleteChangeIsNull(DaAip aip);

    List<DaDao> findByAipAndTypeAndDeleteChangeIsNull(DaAip aip, DaDao.DaoType type);

    List<DaDao> findAllByLevelViewInAndDeleteChangeIsNull(List<DaLevelView> levelView);

    List<DaDao> findAllByAip_AipIdAndDeleteChangeIsNull(Integer aipId);


    List<DaDao> findByAipInAndDeleteChangeIsNull(List<DaAip> aips);

    @Query(
            "SELECT d " +
            "FROM da_dao d " +
            "WHERE d.aip.aipId IN :aipIds " +
                "AND d.type = :type " +
                "AND d.deleteChange IS NULL " +
                "AND d.levelView IS NOT NULL"
    )
    List<DaDao> findByAipIdsAndTypeAndDeleteChangeIsNullAndLevelViewIdIsNotNull(@Param("aipIds") List<Integer> aipIds, @Param("type") DaDao.DaoType type);
}
