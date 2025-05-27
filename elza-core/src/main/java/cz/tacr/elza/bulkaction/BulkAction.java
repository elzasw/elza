package cz.tacr.elza.bulkaction;

public interface BulkAction {

	/**
	 * Inicializace a spuštění akce.
	 * 
	 * @param runContext
	 */
	public void execute(ActionRunContext runContext) throws InterruptedException;

	/**
	 * Vrací jméno akcie.
	 *
	 * Value is used to log result, etc.
	 *
	 * @return
	 */
	public String getName();

	/**
	 *  Přerušení akce
	 */
	public void terminate();
}
