package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ApExternalIdType;

public interface ApExternalIdTypeRepository extends JpaRepository<ApExternalIdType, Integer>, Packaging<ApExternalIdType> {
}
