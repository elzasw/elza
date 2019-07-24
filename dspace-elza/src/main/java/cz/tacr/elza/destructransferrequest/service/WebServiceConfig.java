package cz.tacr.elza.destructransferrequest.service;

import javax.xml.ws.Endpoint;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class WebServiceConfig {

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Autowired
    private DaoRequestsImpl daoRequests;

    @Autowired
    private DaoNotificationsImpl daoNotifications;

//    @Bean
//    public Endpoint daoRequestsWS() {
//        String baseUrl = configurationService.getProperty("dspace.baseUrl");
//
//        return Endpoint.publish(baseUrl + "/ws/DaoRequests", daoRequests);
//    }
//
//    @Bean
//    public Endpoint daoNotificationsWS() {
//        String baseUrl = configurationService.getProperty("dspace.baseUrl");
//
//        return Endpoint.publish(baseUrl + "/ws/DaoNotifications", daoNotifications);
//    }
}
