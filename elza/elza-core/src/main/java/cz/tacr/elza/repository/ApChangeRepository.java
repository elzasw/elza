package cz.tacr.elza.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.UsrUser;

@Repository
public interface ApChangeRepository extends ElzaJpaRepository<ApChange, Integer> {

    ApChange findTop1ByOrderByChangeIdDesc();

    ApChange findTop1ByOrderByChangeIdAsc();

    @Query("""
        select distinct u from ap_change c
        join c.user u
        left join ap_state s on s.createChange = c
		left join ap_rev_state rs on rs.createChange = c
		left join rs.revision rv
		left join rv.state rvs
		where rvs.accessPoint = :ap or s.accessPoint = :ap
		  and u is not null
		  and u.active = true
    """)
    List<UsrUser> findUsersByAccessPoint(ApAccessPoint ap);

    /**
     * Find distinct users who edited parts or items of the given access point
     * after {@code sinceChangeIdExclusive} (null = no cutoff). The result row is
     * (user, max(changeDate)) — one row per user with their most recent edit.
     */
    @Query("""
        select u, max(c.changeDate) from ap_change c
        join c.user u
        where u.active = true
          and (:sinceChangeIdExclusive is null or c.changeId > :sinceChangeIdExclusive)
          and (
               exists (select 1 from ApPart p
                        where p.accessPoint = :ap
                          and (p.createChange = c or p.lastChange = c or p.deleteChange = c))
            or exists (select 1 from ApItem i
                        where i.part.accessPoint = :ap
                          and (i.createChange = c or i.deleteChange = c))
          )
        group by u
    """)
    List<Object[]> findEditorParticipants(@Param("ap") ApAccessPoint ap,
                                          @Param("sinceChangeIdExclusive") Integer sinceChangeIdExclusive);

    /**
     * Find distinct users who approved the given access point (created an
     * ap_state row with stateApproval = APPROVED) after {@code sinceChangeIdExclusive}
     * (null = no cutoff). The result row is (user, max(changeDate)).
     */
    @Query("""
        select u, max(c.changeDate) from ap_state s
        join s.createChange c
        join c.user u
        where s.accessPoint = :ap
          and s.stateApproval = cz.tacr.elza.domain.ApState.StateApproval.APPROVED
          and u.active = true
          and (:sinceChangeIdExclusive is null or c.changeId > :sinceChangeIdExclusive)
        group by u
    """)
    List<Object[]> findApproverParticipants(@Param("ap") ApAccessPoint ap,
                                            @Param("sinceChangeIdExclusive") Integer sinceChangeIdExclusive);

    /**
     * Convenience row shape for participant queries.
     */
    record ParticipantRow(UsrUser user, OffsetDateTime lastChange) {
        public static ParticipantRow from(Object[] row) {
            return new ParticipantRow((UsrUser) row[0], (OffsetDateTime) row[1]);
        }
    }
}
