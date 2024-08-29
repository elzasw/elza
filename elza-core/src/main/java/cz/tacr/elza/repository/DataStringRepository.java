package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;


/**
 * Data repository for arr_data_string
 */
@Repository
public interface DataStringRepository extends JpaRepository<ArrDataString, Integer> {

    public interface OnlyValues {
        Integer getDataId();

        String getStringValue();
    }

    //@Query(nativeQuery = true, value = "select data_id, stringValue from arr_data_string where data_id in (?1)")
    Collection<OnlyValues> findValuesByDataIdIn(List<Integer> ids);

    @Modifying
    @Query(nativeQuery = true, value = "delete from arr_data_string where data_id in (?1)")
    void deleteMasterOnly(List<Integer> ids);

    /**
     * Insert data only in the table arr_data_string.
     * 
     * Data in the {@link ArrData} has to be inserted by other method
     *
     * @param dataId
     * @param stringValue
     */
    @Modifying
    @Query(nativeQuery = true, value = "insert into arr_data_string(data_id, value) values(?1, ?2)")
    void insertMasterOnly(Integer dataId, String stringValue);

}
