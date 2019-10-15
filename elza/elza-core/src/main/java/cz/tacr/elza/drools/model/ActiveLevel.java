package cz.tacr.elza.drools.model;

/**
 * Object representing active level for the Drools.
 * 
 * There is exactly only one active level for each rule check.
 * @author Petr Pytelka
 *
 */
public class ActiveLevel extends Level {
    /**
     * Uzel sourozence před.
     */
    private Level siblingBefore;

    /**
     * Uzel sourozence po.
     */
    private Level siblingAfter;

	public ActiveLevel(Level modelLevel) {
		super(modelLevel);
	}

	public Level getSiblingBefore() {
		return siblingBefore;
	}

	public void setSiblingBefore(Level siblingBefore) {
		this.siblingBefore = siblingBefore;
	}

	public Level getSiblingAfter() {
		return siblingAfter;
	}

	public void setSiblingAfter(Level siblingAfter) {
		this.siblingAfter = siblingAfter;
	}

}
