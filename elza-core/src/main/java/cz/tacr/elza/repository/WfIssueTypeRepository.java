package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssueType;

@Repository
public interface WfIssueTypeRepository extends ElzaJpaRepository<WfIssueType, Integer> {
}