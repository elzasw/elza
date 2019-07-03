package cz.tacr.elza.ws.dao_service.v1;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class was generated by Apache CXF 3.1.11
 * 2019-06-21T09:42:07.180+02:00
 * Generated source version: 3.1.11
 * 
 */
@WebServiceClient(name = "DaoProvider", 
                  wsdlLocation = "file:/C:/Projekty/dspace/dspace-elza/src/main/resources/wsdl/elza-dao-service-v1.wsdl",
                  targetNamespace = "http://elza.tacr.cz/ws/dao-service/v1") 
public class DaoProvider extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://elza.tacr.cz/ws/dao-service/v1", "DaoProvider");
    public final static QName DaoNotificationsPort = new QName("http://elza.tacr.cz/ws/dao-service/v1", "DaoNotificationsPort");
    public final static QName DaoRequests = new QName("http://elza.tacr.cz/ws/dao-service/v1", "DaoRequests");
    static {
        URL url = null;
        try {
            url = new URL("file:/C:/Projekty/dspace/dspace-elza/src/main/resources/wsdl/elza-dao-service-v1.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(DaoProvider.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "file:/C:/Projekty/dspace/dspace-elza/src/main/resources/wsdl/elza-dao-service-v1.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public DaoProvider(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public DaoProvider(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DaoProvider() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    public DaoProvider(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public DaoProvider(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public DaoProvider(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    




    /**
     *
     * @return
     *     returns DaoNotifications
     */
    @WebEndpoint(name = "DaoNotificationsPort")
    public DaoNotifications getDaoNotificationsPort() {
        return super.getPort(DaoNotificationsPort, DaoNotifications.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns DaoNotifications
     */
    @WebEndpoint(name = "DaoNotificationsPort")
    public DaoNotifications getDaoNotificationsPort(WebServiceFeature... features) {
        return super.getPort(DaoNotificationsPort, DaoNotifications.class, features);
    }


    /**
     *
     * @return
     *     returns DaoRequests
     */
    @WebEndpoint(name = "DaoRequests")
    public DaoRequests getDaoRequests() {
        return super.getPort(DaoRequests, DaoRequests.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns DaoRequests
     */
    @WebEndpoint(name = "DaoRequests")
    public DaoRequests getDaoRequests(WebServiceFeature... features) {
        return super.getPort(DaoRequests, DaoRequests.class, features);
    }

}
