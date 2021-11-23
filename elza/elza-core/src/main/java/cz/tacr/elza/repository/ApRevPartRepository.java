package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApRevPartRepository extends JpaRepository<ApRevPart, Integer> {

    @Query("SELECT p FROM ApRevPart p WHERE p.revision = :revision AND p.deleteChange IS NULL")
    List<ApRevPart> findByRevision(@Param("revision") ApRevision revision);

    @Query("SELECT p FROM ApRevPart p WHERE p.parentPart = :parentPart AND p.deleteChange IS NULL")
    List<ApRevPart> findByParentPart(@Param("parentPart") ApPart parentPart);

    @Query("SELECT p FROM ApRevPart p WHERE p.revParentPart = :revParentPart AND p.deleteChange IS NULL")
    List<ApRevPart> findByRevParentPart(@Param("revParentPart") ApRevPart revParentPart);
}
