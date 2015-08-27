package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends JpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

    @Query("select rr from reg_record rr join rr.registerType rt join rr.variantRecordList vr"
            + " where lower(rr.record) like CONCAT('%', :searchRecord, '%') and rt.registerTypeId = :registerTypeId and vr.record like 'x'")
    public List<RegRecord> findByRecordLower(@Param(value = "searchRecord") String searchRecord,
                                             @Param(value = "registerTypeId")  Integer registerTypeId);

    @Query("select rr from reg_variant_record vr join vr.regRecord rr join rr.registerType rt"
            + " where lower(vr.record) like CONCAT('%', :searchRecord, '%') and rt.registerTypeId = :registerTypeId")
    public List<RegRecord> findByVariantRecordLower(@Param(value = "searchRecord") String searchRecord,
                                                    @Param(value = "registerTypeId")  Integer registerTypeId);

}
