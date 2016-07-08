package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyGroup extends Serializable {

    String getScope();

    void setScope(String scope);

    String getFoundingNorm();

    void setFoundingNorm(String foundingNorm);

    String getScopeNorm();

    void setScopeNorm(String scopeNorm);

    String getOrganization();

    void setOrganization(String organization);

}
