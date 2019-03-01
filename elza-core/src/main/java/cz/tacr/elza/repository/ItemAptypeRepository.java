package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

/**
 * ItemAptypeRepository repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.6.2016
 */
public interface ItemAptypeRepository extends ElzaJpaRepository<RulItemAptype, Integer> {

    /**
     * Vrátí registry k dané specifikaci
     *
     * @param itemSpec specifikace
     * @return vrací registry specifikace
     */
    @Query("SELECT ap FROM rul_item_aptype ap WHERE ap.itemSpec = ?1")
    List<RulItemAptype> findByItemSpec(RulItemSpec itemSpec);

    /**
     * Vrátí registry k danému typu
     *
     * @param itemType typ
     * @return vrací registry typu
     */
    @Query("SELECT ap FROM rul_item_aptype ap WHERE ap.itemType = ?1")
    List<RulItemAptype> findByItemType(RulItemType itemType);

    /**
     * Vrátí registry k dané specifikaci
     *
     * @param itemSpec specifikace
     * @return vrací registry specifikace
     */
    @Query("SELECT DISTINCT ap.apType.apTypeId FROM rul_item_aptype ap WHERE ap.itemSpec = ?1")
    List<Integer> findApTypeIdsByItemSpec(RulItemSpec itemSpec);

    /**
     * Vrátí registry k danému typu
     *
     * @param itemType typ
     * @return vrací registry typu
     */
    @Query("SELECT DISTINCT ap.apType.apTypeId FROM rul_item_aptype ap WHERE ap.itemType = ?1")
    List<Integer> findApTypeIdsByItemType(RulItemType itemType);

    void deleteByItemSpec(RulItemSpec itemSpec);

    void deleteByItemType(RulItemType itemType);
}
