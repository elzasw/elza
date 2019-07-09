package cz.tacr.elza.destructransferrequest.service;

import javax.xml.ws.Endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class WebServiceConfig {

    @Autowired
    private DaoRequestsImpl daoRequests;

    @Bean
    public Endpoint daoCoreServiceWS() {
        return Endpoint.publish("http://localhost:8080/ws/DaoCoreRequests", daoRequests);
    }
}
