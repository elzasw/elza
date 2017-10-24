package cz.tacr.elza.controller.arrangement;

/**
 * Texts and other data for form title
 */
public class NodeFormTitle {

	protected final String titleLeft;
	protected final String titleRight;

	NodeFormTitle(String left, String right) {
		this.titleLeft = left;
		this.titleRight = right;
	}

	public String getTitleLeft() {
		return titleLeft;
	}

	public String getTitleRight() {
		return titleRight;
	}

}
