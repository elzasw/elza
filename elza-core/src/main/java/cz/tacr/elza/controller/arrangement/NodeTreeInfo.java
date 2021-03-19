package cz.tacr.elza.controller.arrangement;

/**
 * Description of node in tree
 * 
 */
public class NodeTreeInfo {

    protected String icon;

    protected String name;

	public NodeTreeInfo() {

	}

	public NodeTreeInfo(String icon, String name) {
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
