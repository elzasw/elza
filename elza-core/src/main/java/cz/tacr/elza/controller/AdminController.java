package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.service.AdminService;

/**
 * Kontroler pro administraci.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @RequestMapping(value = "/reindex", method = RequestMethod.GET)
    public void reindex() {
        adminService.reindex();
    }

    @RequestMapping(value = "/reindexStatus", method = RequestMethod.GET)
    public boolean reindexStatus() {
        return adminService.isIndexingRunning();
    }
}
