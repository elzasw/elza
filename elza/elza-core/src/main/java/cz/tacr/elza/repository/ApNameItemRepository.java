package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.service.ByType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApNameItemRepository extends JpaRepository<ApNameItem, Integer>, ByType<ApName> {

    @Query("SELECT fi FROM ApNameItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.name = :name")
    List<ApNameItem> findValidItemsByName(@Param("name") ApName name);

    @Query("SELECT fi FROM ApNameItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.name IN :names")
    List<ApNameItem> findValidItemsByNames(@Param("names") Collection<ApName> names);

    @Modifying
    @Query("DELETE FROM ApNameItem ni WHERE ni.itemId IN (SELECT i.itemId FROM ApNameItem i JOIN i.name n WHERE n.state = 'TEMP')")
    void removeTempItems();

    @Modifying
    @Query("DELETE FROM ApNameItem ni WHERE ni.itemId IN (SELECT i.itemId FROM ApNameItem i JOIN i.name n WHERE n.state = 'TEMP' AND n.accessPoint = :accessPoint)")
    void removeTempItems(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT fi FROM ApNameItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.name = :name AND fi.itemType = :itemType")
    List<ApItem> findValidItemsByType(@Param("name") ApName name, @Param("itemType") RulItemType itemType);
}
