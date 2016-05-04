package cz.tacr.elza.controller.vo;

import java.util.Date;

/**
 * VO verze výstupu archivního souboru.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.04.2016
 */
public class ArrOutputVO {

    /**
     * Id verze.
     */
    private Integer id;
    /**
     * Změna uzavření verze.
     */
    private Date lockDate;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Date getLockDate() {
        return lockDate;
    }

    public void setLockDate(final Date lockDate) {
        this.lockDate = lockDate;
    }
}
