package cz.tacr.elza.bulkaction;

/**
 * Konfigurace hromadné akce.
 *
 */
public interface BulkActionConfig {

	/**
	 * Return name of bulk action
	 *
	 * @return
	 */
	public String getName();

	public String getCode();

	public String getDescription();

	boolean isFastAction();

	/**
	 * Kind of the action, derived from the configuration class without the suffix Config
	 * ("PersistentSort", "FundValidation", "MultiAction", ...): what the client and the core key on
	 * instead of the code, which is the file name of the configuration and unique per installation.
	 */
	default String getType() {
		return getClass().getSimpleName().replaceAll("Config$", "");
	}

	public BulkAction createBulkAction();
}
