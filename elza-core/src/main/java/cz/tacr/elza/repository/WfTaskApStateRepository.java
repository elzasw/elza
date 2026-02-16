package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.WfTask;
import cz.tacr.elza.domain.WfTaskApState;

@Repository
public interface WfTaskApStateRepository extends ElzaJpaRepository<WfTaskApState, Integer> {

	@Query("select ts from wf_task_ap_state ts join ts.task t where ts.state = :state and t.timeClosed is null")
	WfTaskApState findByStateAndTimeClosedIsNull(@Param("state") ApState state);

	@Query("select ts from wf_task_ap_state ts join ts.task t where ts.state in :states and t.timeClosed is null")
	List<WfTaskApState> findByStatesAndTimeClosedIsNull(@Param("states") Collection<ApState> states);

	@Query("select ts from wf_task_ap_state ts join fetch ts.state s join fetch s.apType where ts.task in :tasks")
	List<WfTaskApState> findAllByTaskIn(@Param("tasks") List<WfTask> wfTasks);

	List<WfTaskApState> findAllByState(ApState apState);
}
