package cz.tacr.elza.service.vo;

import java.util.List;

/**
 * Value objekt se seznamem změn.
 *
 * @author Martin Šlapa
 * @since 03.11.2016
 */
public class ChangesResult {

    /**
     * Maximální počet položek v seznamu.
     */
    private Integer maxSize;

    /**
     * Počet přeskočených položek v celkovém seznamu.
     */
    private Integer offset;

    /**
     * Celkový počet položek v seznamu.
     */
    private Integer totalCount;

    /**
     * Je seznam neaktuální?
     */
    private Boolean outdated;

    /**
     * Požadovaná část změn ze seznamu.
     */
    private List<Change> changes;

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

    @Override
    public String toString() {
        return "ChangesResult{" +
                "maxSize=" + maxSize +
                ", offset=" + offset +
                ", totalCount=" + totalCount +
                ", outdated=" + outdated +
                ", changes=" + changes +
                '}';
    }
}
