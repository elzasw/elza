package cz.tacr.elza.filter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import cz.tacr.elza.context.ContextUtils;

/**
 * Filtr pro přesměrování na detail itemu. Používá se při volání z ELZA kde známe jen uuid a ne handle.
 */
public class DaoRedirectFilter implements Filter {

    private static Logger log = Logger.getLogger(DaoRedirectFilter.class);

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String uuid = httpServletRequest.getParameter("uuid");
        String contextPath = httpServletRequest.getContextPath();
        if (uuid == null) {
            log.error("Není předán identifikátor dao/item.");
        } else {
            try {
                log.debug("Požadavek na přesměrování na detail dao s uuid=" + uuid);
                Context context = ContextUtils.createContext();
                Item item = itemService.find(context, UUID.fromString(uuid));

                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                String targetContext;
                if (contextPath.equalsIgnoreCase("/elza_war")) { // velmi hrubý odhad cesty na které beží XMLUI
                    targetContext = "/xmlui_war";
                } else {
                    targetContext = "/xmlui";
                }
                httpServletResponse.sendRedirect(configurationService.getProperty("dspace.baseUrl") + targetContext + "/handle/" + item.getHandle());
            } catch (Exception e) {
                log.error("Chyba při přesměrování na detail dao.", e);
                throw new ServletException(e);
            }
        }
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
