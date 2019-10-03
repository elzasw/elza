package cz.tacr.elza.context;

import java.sql.SQLException;
import java.util.Collection;

import org.dspace.eperson.factory.EPersonServiceFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Třída pro práci s {@link org.dspace.core.Context}
 */
public class ContextUtils {

    /**
     * Vytvoří {@link org.dspace.core.Context} na základě informací v {@link SecurityContextHolder}
     * @return context
     * @throws SQLException
     */
    public static org.dspace.core.Context createContext() throws SQLException {
        // zkopírováno z org.dspace.rest.Resource.createContext
        // další možnost inicializace contextu org.dspace.app.webui.util.UIUtil.obtainContext
        org.dspace.core.Context context = new org.dspace.core.Context();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null)
        {
            Collection<SimpleGrantedAuthority> specialGroups = (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();
            for (SimpleGrantedAuthority grantedAuthority : specialGroups) {
                context.setSpecialGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, grantedAuthority.getAuthority()).getID());
            }
            context.setCurrentUser(EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, authentication.getName()));
        }

        return context;
    }
}
