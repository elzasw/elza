package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.RegCoordinates;

/**
 * Repository pro souřadnice rejstříkových hesel.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 11.11.16
 */
public interface RegCoordinatesRepository extends ElzaJpaRepository<RegCoordinates, Integer> {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT cord FROM reg_coordinates cord JOIN cord.regRecord r WHERE r.recordId = ?1 ORDER BY cord.coordinatesId")
    List<RegCoordinates> findByRegRecordId(Integer recordId);

}
