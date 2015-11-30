package cz.tacr.elza.drools.model;

/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ zm�ny.
 *
 * @author Martin �lapa
 * @since 27.11.2015
 */
public class DescItemVO {

    /**
     * Typ atributu
     */
    private String type;

    /**
     * Typ zm�ny atributu.
     */
    private DescItemChange change;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public DescItemChange getChange() {
        return change;
    }

    public void setChange(final DescItemChange change) {
        this.change = change;
    }
}
