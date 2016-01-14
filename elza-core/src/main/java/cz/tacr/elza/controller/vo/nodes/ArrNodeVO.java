package cz.tacr.elza.controller.vo.nodes;


import java.time.LocalDateTime;


/**
 * VO uzlu archivní pomůcky.
 *
 * @author Martin Šlapa
 * @since 13.1.2016
 */
public class ArrNodeVO {

    /**
     * identifikátor uzlu
     */
    private Integer id;

    /**
     * datum posledné změny
     */
    private LocalDateTime lastUpdate;

    /**
     * jedinečné id
     */
    private String uuid;

    /**
     * verze uzlu
     */
    private Integer version;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
