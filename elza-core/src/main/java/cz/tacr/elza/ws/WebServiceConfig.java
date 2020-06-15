package cz.tacr.elza.ws;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import cz.tacr.elza.ws.core.v1.DaoCoreServiceImpl;
import cz.tacr.elza.ws.core.v1.DaoDigitizationServiceImpl;
import cz.tacr.elza.ws.core.v1.DaoRequestsServiceImpl;
import cz.tacr.elza.ws.core.v1.ExportServiceImpl;
import cz.tacr.elza.ws.core.v1.FundServiceImpl;
import cz.tacr.elza.ws.core.v1.StructuredObjectServiceImpl;

/**
 * CXF Servlet configuration
 * 
 *
 * Sevlet is by default binded to URL/services/...
 * Binding URL can be changed using cxf.servlet.init
 * 
 * Since CXF 3.3 we can use CXF default binding constant
 * see org.apache.cxf.spring.boot.autoconfigure.CxfProperties
 */
@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class WebServiceConfig {

    public final static String DAO_CORE_SERVICE_URL = "/DaoCoreService";
    public final static String STRUCT_OBJ_SERVICE_URL = "/StructuredObjectService";
    public final static String FUND_SERVICE_URL = "/FundService";

    @Autowired
    private DaoDigitizationServiceImpl daoDigitizationService;

    @Autowired
    private DaoCoreServiceImpl daoCoreService;

    @Autowired
    private DaoRequestsServiceImpl daoRequestsService;

    @Autowired
    private ExportServiceImpl exportService;

    @Autowired
    private FundServiceImpl fundService;

    @Autowired
    private StructuredObjectServiceImpl structuredObjectService;

    @Autowired
    private SpringBus bus;

    @PostConstruct
    private void init() {
        final FaultInterceptor faultInterceptor = new FaultInterceptor();
        bus.getOutFaultInterceptors().add(faultInterceptor);
    }

    @Bean
    public Endpoint daoDigitizationServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoDigitizationService);
        endpoint.publish("/DaoDigitizationService");
        return endpoint;
    }

    @Bean
    public Endpoint daoRequestsServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoRequestsService);
        endpoint.publish("/DaoRequestsService");
        return endpoint;
    }

    @Bean
    public Endpoint daoCoreServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, daoCoreService);
        endpoint.publish(DAO_CORE_SERVICE_URL);
        return endpoint;
    }

    @Bean
    public Endpoint entitiesServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, exportService);
        endpoint.publish("/ExportService");

        Binding binding = endpoint.getBinding();
        List<Handler> hc = binding.getHandlerChain();
        hc.add(new CustomSOAPHandler());
        binding.setHandlerChain(hc);

        return endpoint;
    }

    @Bean
    public Endpoint structObjServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, structuredObjectService);
        endpoint.publish(STRUCT_OBJ_SERVICE_URL);

        Binding binding = endpoint.getBinding();
        List<Handler> hc = binding.getHandlerChain();
        hc.add(new CustomSOAPHandler());
        binding.setHandlerChain(hc);

        return endpoint;
    }

    @Bean
    public Endpoint fundServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, fundService);
        endpoint.publish(FUND_SERVICE_URL);

        Binding binding = endpoint.getBinding();
        List<Handler> hc = binding.getHandlerChain();
        hc.add(new CustomSOAPHandler());
        binding.setHandlerChain(hc);

        return endpoint;
    }
}
