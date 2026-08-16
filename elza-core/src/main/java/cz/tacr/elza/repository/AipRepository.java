package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AipRepository extends ElzaJpaRepository<DaAip, Integer>, AipRepositoryCustom {

    DaAip findByCode(String code);

    @Query("select a from da_aip a where a.aipId in :aipIds and not exists (select dl from arr_da_link dl where dl.aip = a and dl.deleteChange is null)")
    List<DaAip> findByIdAndLinkNotExists(@Param("aipIds") List<Integer> aipIds);

    @Query("select a from da_aip a where a.aipId in :aipIds and exists (select dl from arr_da_link dl where dl.aip = a and dl.deleteChange is null)")
    List<DaAip> findByIdAndLinkExists(@Param("aipIds") List<Integer> aipIds);

    List<DaAip> findByCodeIn(List<String> codes);
}
