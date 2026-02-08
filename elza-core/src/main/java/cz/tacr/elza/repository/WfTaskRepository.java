package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfTask;

@Repository
public interface WfTaskRepository extends ElzaJpaRepository<WfTask, Integer> {

	@Query("select t from wf_task t join fetch t.taskType where t.assigneeId = :assigneeId")
	List<WfTask> findAllByAssigneeId(@Param("assigneeId") Integer assigneeId);
}
