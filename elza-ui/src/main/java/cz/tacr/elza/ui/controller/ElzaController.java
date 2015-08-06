package cz.tacr.elza.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 6.8.15
 */
@Controller(value = "/")
public class ElzaController {

    @RequestMapping("/")
    public String index() {
        return "redirect:/ui";
    }

}
