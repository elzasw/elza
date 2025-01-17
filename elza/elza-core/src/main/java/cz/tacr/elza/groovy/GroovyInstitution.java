package cz.tacr.elza.groovy;

import cz.tacr.elza.domain.ParInstitution;

public class GroovyInstitution {

	private final String internalCode;

	private final GroovyAe groovyAe;

	public GroovyInstitution(ParInstitution institution, GroovyAe groovyAe) {
		this.internalCode = institution.getInternalCode();
		this.groovyAe = groovyAe;
	}

	public String getInternalCode() {
		return internalCode;
	}

	public GroovyAe getGroovyAe() {
		return groovyAe;
	}
}
