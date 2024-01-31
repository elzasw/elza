package cz.tacr.elza.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.service.AccessPointService;

/**
 * Kontroler pro ELZA UI - React stránky.
 *
 */
@Controller
@PropertySource(value = "classpath:/META-INF/maven/cz.tacr.elza/elza-core/pom.properties", ignoreResourceNotFound = true)
public class ElzaWebController {

    static private final Logger logger = LoggerFactory.getLogger(ElzaWebController.class);

    @Autowired
    ApTypeRepository apTypeRepository;

    @Autowired
    AccessPointService accessPointService;

    @Value("${spring.app.buildType}")
    private String buildType;

    @ModelAttribute("isDevBuild")
    public Boolean isDevBuild() {
        return "DEV".equals(buildType);
    }

    @Value("${elza.security.allowDefaultUser:true}")
    private Boolean allowDefaultUser;

    @ModelAttribute("isDefaultUserEnabled")
    public Boolean isDefaultUserEnabled() {
        return allowDefaultUser;
    }

    /**
     * Display logs in the browser console
     */
    @Value("${elza.debug.clientLog:false}")
    private Boolean clientLog;

    @ModelAttribute("isClientLog")
    public Boolean isClientLog() {
        return clientLog;
    }

    /**
     * Flag if output can be send to other system
     */
    @Value("${elza.output.allowSend:false}")
    private Boolean allowSendOutput;

    @ModelAttribute("isSendOutputEnabled")
    public Boolean isSetOutputEnabled() {
        return allowSendOutput;
    }

    @Value("${elza.security.displayUserInfo:true}")
    private Boolean displayUserInfo;

    @ModelAttribute("displayUserInfo")
    public Boolean getDisplayUserInfo() {
        return displayUserInfo;
    }

    @Value("${version:0.0.0}")
    private String appVersion;

    @ModelAttribute("appVersion")
    public String getAppVersion() {
    	return appVersion;
    }

    @Value("${elza.security.logoutUrl:}")
    private String logoutUrl;

    @Value("${elza.appName:ELZA}")
    private String appName;

    @ModelAttribute("appName")
    public String getAppName() {
        return StringUtils.isBlank(appName) ? "ELZA" : appName;
    }

    @ModelAttribute("logoutUrl")
    public String getLogoutUrl() {
        return logoutUrl;
    }

    @Value("${elza.integrationScriptUrl:}")
    private String integrationScriptUrl;

    @ModelAttribute("integrationScriptUrl")
    public String getIntegrationScriptUrl() {
        return integrationScriptUrl;
    }

    @ModelAttribute("hasIntegrationScriptUrl")
    public boolean hasIntegrationScriptUrl() {
        return StringUtils.isNotBlank(integrationScriptUrl);
    }

    private void initDefaults(final HttpServletRequest request, final Model model) {
        model.addAttribute("contextPath", request.getContextPath());
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String indexPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String errorPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "error";
    }

    @RequestMapping(value = "/arr/**", method = RequestMethod.GET)
    public String arrPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/fund/**", method = RequestMethod.GET)
    public String fundPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/node/**", method = RequestMethod.GET)
    public String nodePage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/entity/**", method = RequestMethod.GET)
    public String entityPage(final HttpServletRequest request, final Model model) {
        // check entity id
        String path = request.getRequestURI();
        String pathItems[] = path.split("/");
        String requestedItemId = pathItems[pathItems.length - 1];
        // for revisions we have to read prev item
        if ("revision".equals(requestedItemId)) {
            requestedItemId = pathItems[pathItems.length - 2];
        }

        if (StringUtils.isNotBlank(requestedItemId) && !requestedItemId.equals("entity")) {
            ApAccessPoint ap = accessPointService.getAccessPointByIdOrUuid(requestedItemId);
            String itemId = ap.getAccessPointId().toString();
            if (!itemId.equals(requestedItemId)) {
                return "redirect:" + itemId;
            }
        }

        // prepare model
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/entity-create", method = RequestMethod.GET)
    public String entityCreatePage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);

        String entityClass = request.getParameter("entity-class");
        String response = request.getParameter("response");

        if (entityClass != null) {
            ApType apType = apTypeRepository.findApTypeByCode(entityClass);
            if (apType == null) {
                response = response.replace("{status}", "CANCEL")
                            .replace("{entityUuid}", "")
                            .replace("{entityId}", "");
                logger.error("Entity-class {} not found in ap_type.code", entityClass);
                return "redirect:" + response;
            }
        }

        return "web";
    }

    @RequestMapping(value = "/registry", method = RequestMethod.GET)
    public String recordPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/map", method = RequestMethod.GET)
    public String mapPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/admin/**", method = RequestMethod.GET)
    public String adminPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }
}
