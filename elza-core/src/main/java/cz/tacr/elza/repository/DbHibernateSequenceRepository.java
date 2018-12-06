package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DbHibernateSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DbHibernateSequenceRepository extends JpaRepository<DbHibernateSequence, String> {

    @Modifying
    @Query("UPDATE DbHibernateSequence SET nextVal = :nextVal WHERE sequenceName = :sequenceName AND nextVal = :oldVal")
    int setNextVal(@Param("sequenceName") String sequenceName, @Param("nextVal") long nextVal, @Param("oldVal") long oldVal);
}
