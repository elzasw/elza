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

	public String getDescription();

	boolean isFastAction();

	abstract public BulkAction createBulkAction();
}
