package cz.tacr.elza.dao.api.ws;

import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.util.StringUtils;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class WebServiceConfig {

	@Autowired
	private DaoNotifications daoNotifications;

	@Autowired
	private DaoRequests daoRequests;

	@Autowired
	private DCStorageConfig storageConfig;

	@Autowired
	private Bus bus;

	@Bean
	public EndpointImpl notificationsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoNotifications);
		endpoint.publish(getWebServiceName(DaoNotifications.class));
		return endpoint;
	}

	@Bean
	public EndpointImpl requestsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoRequests);
		endpoint.publish(getWebServiceName(DaoRequests.class));
		return endpoint;
	}

	@Bean
	public ServletRegistrationBean CXFServlet() {
		String contextPath = "/" + storageConfig.getRepositoryIdentifier() + "/api/*";
		ServletRegistrationBean servlet = new ServletRegistrationBean(new CXFServlet(), contextPath);
		servlet.setLoadOnStartup(1);
		return servlet;
	}

	private static String getWebServiceName(Class<?> serviceClass) {
		WebService[] webServices = serviceClass.getDeclaredAnnotationsByType(WebService.class);
		for (int i = 0; i < webServices.length; i++) {
			String name = webServices[i].name();
			if (StringUtils.hasText(name)) {
				return name;
			}
		}
		throw new RuntimeException("@WebService name not found");
	}
}