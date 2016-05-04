package cz.tacr.elza.controller.vo;

import java.util.Date;

/**
 * VO rozšířené verze výstupu archivního souboru.
 *
 * @author Martin Šlapa
 * @since 03.05.2016
 */
public class ArrOutputExtVO extends ArrOutputVO {

    /**
     * Pojmenovený výstup z AS.
     */
    private ArrNamedOutputVO namedOutput;

    /**
     * Změna vytvoření verze.
     */
    private Date createDate;

    public ArrNamedOutputVO getNamedOutput() {
        return namedOutput;
    }

    public void setNamedOutput(final ArrNamedOutputVO namedOutput) {
        this.namedOutput = namedOutput;
    }

    public Date getCreateDate() {
        return createDate;
    }
    
    public void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }

}
