package cz.tacr.elza.drools.model;

/**
 * Strategy of checks
 * @author Petr Pytelka
 *
 */
public class Strategy {
	/**
	 * Strategy code
	 */
	String code;

	public Strategy(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}	
}
