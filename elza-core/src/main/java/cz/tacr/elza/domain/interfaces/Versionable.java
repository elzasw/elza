package cz.tacr.elza.domain.interfaces;

/**
 * Rozhraní pro objekty které budou mít verzi sloužící jako optimistický zámek.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
public interface Versionable {

    Integer getVersion();

    void setVersion(Integer version);
}
