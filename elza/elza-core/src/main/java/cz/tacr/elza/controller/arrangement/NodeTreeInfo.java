package cz.tacr.elza.controller.arrangement;

/**
 * Description of node in tree
 * 
 */
public class NodeTreeInfo {
	protected final String icon;
	protected final String name;

	NodeTreeInfo(String icon, String name) {
		this.icon = icon;
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public String getName() {
		return name;
	}

}
