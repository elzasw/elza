package cz.tacr.elza.bulkaction;

public abstract class BaseActionConfig implements BulkActionConfig {

	/**
	 * Kód hromadné akce.
	 *
	 */
	protected String code;

	protected String name;

	protected String description;

	protected boolean fastAction = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Vrací kód hromadné akce.
	 *
	 * @return kód hromadné akce
	 */
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public boolean isFastAction() {
		return fastAction;
	}

	public void setFastAction(boolean fastAction) {
		this.fastAction = fastAction;
	}

	@Override
	public String toString() {
		return "BulkActionConfig{" + "code='" + code + '\'' + '}';
	}
}
