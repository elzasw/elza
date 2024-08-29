package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.repository.DataStringRepository.OnlyValues;


/**
 * 
 * @since 20.8.2015
 */
@Repository
public interface DataTextRepository extends JpaRepository<ArrDataText, Integer> {

    public interface OnlyValues {
        Integer getDataId();

        String getTextValue();
    }

    Collection<OnlyValues> findValuesByDataIdIn(List<Integer> ids);

    @Modifying
    @Query(nativeQuery = true, value = "delete from arr_data_text where data_id in (?1)")
    void deleteMasterOnly(List<Integer> ids);
}
