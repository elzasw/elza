package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApNameItemRepository extends JpaRepository<ApNameItem, Integer> {

    @Query("SELECT fi FROM ApNameItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.name = :name")
    List<ApNameItem> findValidItemsByName(@Param("name") ApName name);

    @Query("SELECT fi FROM ApNameItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.name IN :names")
    List<ApNameItem> findValidItemsByNames(@Param("names") Collection<ApName> names);
}
