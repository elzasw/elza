package cz.tacr.elza.config;

import javax.annotation.PostConstruct;
import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import cz.tacr.elza.destructransferrequest.service.DaoNotificationsImpl;
import cz.tacr.elza.destructransferrequest.service.DaoRequestsImpl;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;


@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml"})
public class JaxWsConfiguration {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Bus bus;

    @Autowired
    private DaoRequests daoRequests;

    @Autowired
    private DaoNotifications daoNotifications;

    /**
     * Zavola se pri prvnim pristupu na CXF servlet (/ws/*).
     */
    private void lazyStartServices() {
        startServices();
    }



    /**
     * Ze spring kontextu vezme beany s anotaci {@link WebService} a publikuje je pomocí CXF.
     */
    public void startServices() {
        try {
            startService(daoRequests, DaoRequestsImpl.class, "DaoRequests");
            startService(daoNotifications, DaoNotificationsImpl.class, "DaoNotifications");
        } catch (Exception e) {
            logger.error("Webovou službu " + DaoRequestsImpl.class.getName() + " nelze spustit "
                    + e.getMessage(), e);
        }
    }

    @PostConstruct
    private void initBus() {
        bus.getInInterceptors().add(new LoggingInInterceptor());
        bus.getOutInterceptors().add(new LoggingOutInterceptor());

        bus.getFeatures().add(new FastInfosetFeature());

        startServices();
    }

    private void startService(Object service, Class<?> serviceClass, String url) {
        JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean();
        serverFactoryBean.setServiceClass(serviceClass);
        serverFactoryBean.setServiceBean(service);
        serverFactoryBean.setAddress("/" + url);
        serverFactoryBean.setBus(bus);

        try {
            serverFactoryBean.create();
        }catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.getCause().toString() : ex.toString();
            logger.error("Chyba při spuštění webové služby " + serverFactoryBean.getAddress() + ": " + msg);
        }
    }
}
