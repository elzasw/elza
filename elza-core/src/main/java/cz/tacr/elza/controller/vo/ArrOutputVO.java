package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.Objects;

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrOutputVO that = (ArrOutputVO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(lockDate, that.lockDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lockDate);
    }
}
