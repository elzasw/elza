package cz.tacr.elza.ws;

import cz.tacr.elza.ws.core.v1.DaoCoreServiceImpl;
import cz.tacr.elza.ws.core.v1.DaoDigitizationServiceImpl;
import cz.tacr.elza.ws.core.v1.DaoRequestsServiceImpl;
import org.apache.cxf.Bus;
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
    private DaoDigitizationServiceImpl daoDigitizationService;

    @Autowired
    private DaoCoreServiceImpl daoCoreService;

    @Autowired
    private DaoRequestsServiceImpl daoRequestsService;

    @Autowired
    private Bus bus;

    @Bean
    public Endpoint daoDigitizationServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoDigitizationService);
        endpoint.publish("/DaoDigitizationService");
        return endpoint;
    }

    @Bean
    public Endpoint daoRequestsServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoCoreService);
        endpoint.publish("/DaoRequestsService");
        return endpoint;
    }

    @Bean
    public Endpoint daoCoreServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoRequestsService);
        endpoint.publish("/DaoCoreService");
        return endpoint;
    }

}
