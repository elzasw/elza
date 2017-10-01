package cz.tacr.elza.bulkaction;

/**
 * Konfigurace hromadné akce.
 *
 */
public abstract interface BulkActionConfig {

	/**
	 * Return name of bulk action
	 * 
	 * @return
	 */
	public String getName();

	public String getCode();

	public void setCode(String code);

	public String getRules();

	public String getDescription();

	abstract public BulkAction createBulkAction();
}
