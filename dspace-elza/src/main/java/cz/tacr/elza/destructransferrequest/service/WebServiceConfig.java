package cz.tacr.elza.destructransferrequest.service;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.xml.ws.Endpoint;


@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class WebServiceConfig {

    @Autowired
    private DaoRequestsImpl daoRequests;

    @Autowired
    private SpringBus bus;

    @Bean
    public Endpoint daoCoreServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoRequests);
        endpoint.publish("/DaoCoreRequests");
        return endpoint;
    }

}
