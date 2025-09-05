package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfTaskType;

@Repository
public interface WfTaskTypeRepository extends ElzaJpaRepository<WfTaskType, Integer>, Packaging<WfTaskType> {

	WfTaskType findByCode(String taskTypeCode);
}