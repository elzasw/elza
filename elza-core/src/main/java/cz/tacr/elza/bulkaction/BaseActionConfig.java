package cz.tacr.elza.bulkaction;

public abstract class BaseActionConfig implements BulkActionConfig {

	/**
	 * Kód hromadné akce.
	 *
	 */
	protected String code;

	protected String name;

	protected String description;

	protected String codeTypeBulkAction;

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

	public String getCodeTypeBulkAction() {
		return codeTypeBulkAction;
	}

	public void setCodeTypeBulkAction(String codeTypeBulkAction) {
		this.codeTypeBulkAction = codeTypeBulkAction;
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
	public String toString() {
		return "BulkActionConfig{" + "code='" + code + '\'' + '}';
	}
}
