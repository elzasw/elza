package cz.tacr.elza.bulkaction.generator.multiple;

/**
 * Configuration of single action
 */
public interface ActionConfig {

	/**
	 * Return main class for action.
	 * 
	 * Returned class will be used for action instance.
	 * 
	 * @return
	 */
	public Class<? extends Action> getActionClass();
}
