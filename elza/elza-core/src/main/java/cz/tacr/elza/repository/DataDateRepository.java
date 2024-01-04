package cz.tacr.elza.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;


/**
 * Repository pro {@link ArrDataDate}
 *
 * @since 01.06.2018
 */
@Repository
public interface DataDateRepository extends JpaRepository<ArrDataDate, Integer> {

    public interface OnlyValues {
        Integer getDataId();

        LocalDate getValue();
    }

    Collection<OnlyValues> findValuesByDataIdIn(List<Integer> ids);

    /**
     * Insert data only in the table arr_data_date.
     * 
     * Data in the {@link ArrData} has to be inserted by other method
     *
     * @param dataId
     * @param locDate
     */
    @Modifying
    @Query(nativeQuery = true, value = "insert into arr_data_date(data_id, date_value) values(?1, ?2)")
    void insertMasterOnly(Integer dataId, LocalDate locDate);

}
