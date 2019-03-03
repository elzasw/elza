package cz.tacr.elza.controller.vo;

/**
 * Vysledek fultextoveho vyhledavani pres vsechny AS.
 */
public class ArrFundFulltextResult extends ArrFundBaseVO {

    /**
     * Počet JP nalezených v AS.
     */
    private int count;

    /**
     * Identifikátor otevřené verze AS.
     */
    private int fundVersionId;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public int getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final int fundVersionId) {
        this.fundVersionId = fundVersionId;
    }
}
