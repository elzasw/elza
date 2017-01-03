package cz.tacr.elza.controller.vo;

import java.util.List;

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
    private List<InterpiMappingItemVO> mappings;

    public InterpiMappingVO(final Integer partyTypeId, final List<InterpiMappingItemVO> mappings) {
        Assert.notNull(partyTypeId);

        this.partyTypeId = partyTypeId;
        this.mappings = mappings;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public List<InterpiMappingItemVO> getMappings() {
        return mappings;
    }
}
