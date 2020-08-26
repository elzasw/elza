package cz.tacr.elza.repository.vo;

/**
 * Pomocná struktura pro minimalistické vytažení dat z databáze.
 */
public class ItemChange {

    /**
     * Identifikátor doméhoného objektu.
     */
    private Integer id;

    /**
     * Identifikátor související změny (obecně zakládací).
     */
    private Integer changeId;

    public ItemChange(final Integer id, final Integer changeId) {
        this.id = id;
        this.changeId = changeId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getChangeId() {
        return changeId;
    }
}
