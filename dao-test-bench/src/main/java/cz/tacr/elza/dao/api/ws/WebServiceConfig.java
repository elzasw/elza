package cz.tacr.elza.dao.api.ws;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import cz.tacr.elza.dao.DCStorageConfig;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class WebServiceConfig {

	@Autowired
	private DaoNotificationsImpl daoNotifications;

	@Autowired
	private DaoRequestsImpl daoRequests;

	@Autowired
	private DigitizationFrontdeskImpl digitizationFrontdesk;

	@Autowired
	private DCStorageConfig storageConfig;

	@Autowired
	private Bus bus;

	@Bean
	public EndpointImpl daoNotificationsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoNotifications);
		endpoint.publish(DaoNotificationsImpl.NAME);
		return endpoint;
	}

	@Bean
	public EndpointImpl daoRequestsEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, daoRequests);
		endpoint.publish(DaoRequestsImpl.NAME);
		return endpoint;
	}

	@Bean
	public EndpointImpl digitizationFrontdeskEndpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, digitizationFrontdesk);
		endpoint.publish(DigitizationFrontdeskImpl.NAME);
		return endpoint;
	}

	@Bean
	public ServletRegistrationBean CXFServlet() {
		String contextPath = "/" + storageConfig.getRepositoryIdentifier() + "/ws/*";
		ServletRegistrationBean servlet = new ServletRegistrationBean(new CXFServlet(), contextPath);
		servlet.setLoadOnStartup(1);
		return servlet;
	}
}