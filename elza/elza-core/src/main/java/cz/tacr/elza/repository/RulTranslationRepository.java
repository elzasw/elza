package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulTranslation;

@Repository
public interface RulTranslationRepository extends JpaRepository<RulTranslation, Integer>, Packaging<RulTranslation> {

    @Query("SELECT t FROM rul_translation t JOIN FETCH t.rulPackage JOIN FETCH t.language")
    List<RulTranslation> findAllFetchPackageAndLanguage();

    @Query("SELECT t FROM rul_translation t JOIN FETCH t.language WHERE t.rulPackage = ?1"
            + " ORDER BY t.entityType, t.entityCode, t.field")
    List<RulTranslation> findByRulPackageOrdered(RulPackage rulPackage);

}
