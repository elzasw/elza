package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegCoordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository pro souřadnice rejstříkových hesel.
 *
 * @author Petr Compel
 */
public interface RegCoordinatesRepository extends JpaRepository<RegCoordinates, Integer> {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT cord FROM reg_coordinates cord JOIN cord.regRecord r WHERE r.recordId = ?1 ORDER BY cord.coordinatesId")
    List<RegCoordinates> findByRegRecordId(Integer recordId);

}
