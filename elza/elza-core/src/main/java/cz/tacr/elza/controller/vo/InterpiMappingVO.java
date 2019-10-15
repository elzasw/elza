package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import org.springframework.util.Assert;

/**
 * VO pro mapování vztahů INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 1. 2017
 */
public class InterpiMappingVO {

    /** Id typu osoby. */
    private Integer partyTypeId;

    /** Mapování vztahů a entit. */
    private List<InterpiRelationMappingVO> mappings;

    private ExternalRecordVO externalRecord;

    public InterpiMappingVO(final Integer partyTypeId, final List<InterpiRelationMappingVO> mappings, final ExternalRecordVO externalRecord) {
        Assert.notNull(partyTypeId, "Identifikátor typu osoby musí být vyplněn");

        this.partyTypeId = partyTypeId;
        this.mappings = mappings;
        this.externalRecord = externalRecord;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public List<InterpiRelationMappingVO> getMappings() {
        return mappings;
    }

    public ExternalRecordVO getExternalRecord() {
        return externalRecord;
    }

    public void setExternalRecord(ExternalRecordVO externalRecord) {
        this.externalRecord = externalRecord;
    }
}
