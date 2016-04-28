package cz.tacr.elza.web.controller;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Kontroler pro ELZA UI - React stránky.
 *
 * @since 02.12.2015
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 */
@Controller
public class ElzaWebController {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.app.buildType}")
    private String buildType;

    @ModelAttribute("isDevBuild")
    public Boolean isDevBuild() {
        return "DEV".equals(buildType);
    }

    private void initDefaults(final HttpServletRequest request, final Model model) {
        model.addAttribute("contextPath", request.getContextPath());
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String indexPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/arr/**", method = RequestMethod.GET)
    public String arrPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }
    
    @RequestMapping(value = "/fund", method = RequestMethod.GET)
    public String fundPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/registry", method = RequestMethod.GET)
    public String recordPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/party", method = RequestMethod.GET)
    public String partyPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }

    @RequestMapping(value = "/admin/**", method = RequestMethod.GET)
    public String adminPage(final HttpServletRequest request, final Model model) {
        initDefaults(request, model);
        return "web";
    }
}
