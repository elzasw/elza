package cz.tacr.elza.groovy;

public class ResultBuilder {

	private StringBuilder result;

	private String separator = ", ";

	public ResultBuilder() {
		this.result = new StringBuilder();
	}

	public ResultBuilder(String init) {
		this.result = new StringBuilder(init);
	}

	public ResultBuilder(GroovyItem init) {
		this.result = new StringBuilder(init.getValue());
	}

	public ResultBuilder setSeparator(String separator) {
		this.separator = separator;
		return this;
	}

	public ResultBuilder append(String item) {
		result.append(separator).append(item);
		return this;
	}

	public ResultBuilder append(GroovyItem item) {
		if (item != null) {
			result.append(separator).append(item.getValue());
		}
		return this;
	}

	public ResultBuilder append(GroovyItem item, int lengthLimit) {
		if (item != null) {
			String itemString = item.getValue();
	    	if (itemString.length() > lengthLimit) {
	    		itemString = itemString.substring(0, lengthLimit);
	    	}
			result.append(separator).append(itemString);
		}
		return this;
	}

	public ResultBuilder append(String prefix, GroovyItem item) {
		if (item != null) {
			result.append(separator).append(prefix).append(item.getValue());
		}
		return this;
	}

	@Override
	public String toString() {
		return result.toString();
	}
}
