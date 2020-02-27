package cz.tacr.elza.api;

/**
 * Výčet externích systémů pro rejstříky/osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
public enum ApExternalSystemType {

    INTERPI,DEFAULT //TODO gotzy : DEFAULT je placeholder k INTERPI - to nemazat, když je v DB sys_external_system uložený INTERPI(viz. setType(ApExternalSystem type) entity ApExternalSystem
}
