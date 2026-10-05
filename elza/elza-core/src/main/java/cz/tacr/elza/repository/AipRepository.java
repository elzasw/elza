package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface AipRepository extends ElzaJpaRepository<DaAip, Integer>, AipRepositoryCustom {

    DaAip findByCode(String code);

    @Query("select a from da_aip a where a.aipId in :aipIds and not exists (select dl from arr_da_link dl where dl.aip = a and dl.deleteChange is null)")
    List<DaAip> findByIdAndLinkNotExists(@Param("aipIds") List<Integer> aipIds);

    @Query("select a from da_aip a where a.aipId in :aipIds and exists (select dl from arr_da_link dl where dl.aip = a and dl.deleteChange is null)")
    List<DaAip> findByIdAndLinkExists(@Param("aipIds") List<Integer> aipIds);

    List<DaAip> findByCodeIn(List<String> codes);

    /**
     * Locks the AIPs until the end of the transaction, so that requests for the same AIP are queued
     * one after another: a request supersedes the pending one only if it sees it committed. Two
     * exports submitted at once (a double click) each queued their own item and both stayed active.
     * Locked in id order, so two transactions locking overlapping AIPs never wait crosswise.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from da_aip a where a.aipId in :aipIds order by a.aipId")
    List<DaAip> lockByIds(@Param("aipIds") Collection<Integer> aipIds);
}
