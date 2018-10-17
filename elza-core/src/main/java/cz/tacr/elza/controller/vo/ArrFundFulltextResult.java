package cz.tacr.elza.controller.vo;

/**
 * Vysledek fultextoveho vyhledavani pres vsechny AS.
 */
public class ArrFundFulltextResult extends ArrFundBaseVO {

    /**
     * Počet JP nalezených v AS.
     */
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }
}
