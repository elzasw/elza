package cz.tacr.elza.repository.vo;

public class UsedItemTypeVO {

    private final Integer rulItemTypeId;
    private final Long count;
    
    public UsedItemTypeVO(Integer rulItemTypeId, Long count) {
        this.rulItemTypeId = rulItemTypeId;
        this.count = count;
    }

    public Integer getRulItemTypeId() {
        return rulItemTypeId;
    }

    public Long getCount() {
        return count;
    }
}
