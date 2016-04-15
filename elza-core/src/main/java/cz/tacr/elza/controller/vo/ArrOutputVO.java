package cz.tacr.elza.controller.vo;

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
    private ArrChangeVO lockChange;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ArrChangeVO getLockChange() {
        return lockChange;
    }

    public void setLockChange(final ArrChangeVO lockChange) {
        this.lockChange = lockChange;
    }
}
