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
	 * Kind of the action - the YAML tag of its configuration without "!" ("PersistentSort",
	 * "FundValidation", "MultiAction", ...): what the client and the core key on instead of the code,
	 * which is the file name of the configuration and unique per installation. Null for a broken
	 * action.
	 */
	default String getType() {
		return BulkActionConfigManager.getKind(getClass());
	}

	public BulkAction createBulkAction();
}
