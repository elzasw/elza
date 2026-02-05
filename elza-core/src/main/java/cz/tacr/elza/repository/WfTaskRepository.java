package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.WfTask;

@Repository
public interface WfTaskRepository extends ElzaJpaRepository<WfTask, Integer> {

	@Query("select t from wf_task t join fetch t.taskType where t.assigneeId = :assigneeId")
	List<WfTask> findAllByAssigneeId(@Param("assigneeId") Integer assigneeId);

	@Query("""
		select t from wf_task t 
		join wf_task_ap_state s on s.task = t
		join wf_task_ap_rev_state rs on rs.task = t
		where s.state = :state or rs.state.revision.state = :apState
	""")
	List<WfTask> findAllByApState(ApState state);
}
