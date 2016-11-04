package cz.tacr.elza.service.vo;

import java.util.List;

/**
 * TODO: vyplnit popis třídy
 *
 * @author Martin Šlapa
 * @since 03.11.2016
 */
public class ChangesResult {

    private Integer maxSize;

    private Integer offset;

    private Boolean outdated;

    private List<Change> changes;

    private Integer totalCount;

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(final Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(final Integer offset) {
        this.offset = offset;
    }

    public Boolean getOutdated() {
        return outdated;
    }

    public void setOutdated(final Boolean outdated) {
        this.outdated = outdated;
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void setChanges(final List<Change> changes) {
        this.changes = changes;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final Integer totalCount) {
        this.totalCount = totalCount;
    }
}
