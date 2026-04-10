package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.RptCategory;

public interface RptCategoryRepository extends JpaRepository<RptCategory, Integer> {

}