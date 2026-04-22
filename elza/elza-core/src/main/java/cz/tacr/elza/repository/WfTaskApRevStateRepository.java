package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApRevState;
import cz.tacr.elza.domain.WfTask;
import cz.tacr.elza.domain.WfTaskApRevState;

@Repository
public interface WfTaskApRevStateRepository extends ElzaJpaRepository<WfTaskApRevState, Integer> {

	@Query("select rs from wf_task_ap_rev_state rs join rs.task t where rs.state = :revState and t.timeClosed is null")
	WfTaskApRevState findByStateAndTimeClosedIsNull(@Param("revState") ApRevState revState);

	@Query("select rs from wf_task_ap_rev_state rs join rs.task t where rs.state in :revStates and t.timeClosed is null")
	List<WfTaskApRevState> findByStatesAndTimeClosedIsNull(@Param("revStates") Collection<ApRevState> revState);

	@Query("select trs from wf_task_ap_rev_state trs join fetch trs.state rs join fetch rs.revision r join fetch r.state s join fetch s.apType where trs.task in :tasks")
	List<WfTaskApRevState> findAllByTaskIn(@Param("tasks") List<WfTask> wfTasks);

	/**
	 * Returns distinct AP state ids whose active revision has an open task assigned
	 * to the given user. Used by registry search to pre-resolve the assignedTo filter
	 * into an IN list. The pageable is expected to carry a LIMIT so the caller can
	 * detect overflow.
	 */
	@Query("select distinct r.stateId from wf_task_ap_rev_state trs "
			+ "join trs.state rs join rs.revision r join trs.task t "
			+ "where t.assigneeId = :assigneeId and t.timeClosed is null "
			+ "and r.deleteChangeId is null and rs.deleteChangeId is null")
	List<Integer> findApStateIdsByRevisionAssignee(@Param("assigneeId") Integer assigneeId, Pageable pageable);
}
