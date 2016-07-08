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
     * verze uzlu
     */
    private Integer version;

    public ArrNodeVO() {
    }

    public ArrNodeVO(final Integer id, final Integer version) {
        this.id = id;
        this.version = version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
