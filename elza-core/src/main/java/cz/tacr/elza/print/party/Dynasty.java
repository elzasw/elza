package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParDynasty;

/**
 * Dynasty
 */
public class Dynasty extends Party {

    private String genealogy;
    
    private Dynasty(ParDynasty parDynasty, PartyInitHelper initHelper)
    {
    	super(parDynasty, initHelper);
    	this.genealogy = parDynasty.getGenealogy();
    }

    public String getGenealogy() {
        return genealogy;
    }


	public static Dynasty newInstance(ParDynasty parDynasty, PartyInitHelper initHelper) {
		Dynasty dynasty = new Dynasty(parDynasty, initHelper);
		return dynasty;
	}
}
