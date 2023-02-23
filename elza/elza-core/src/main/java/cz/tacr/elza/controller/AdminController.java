package cz.tacr.elza.controller;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.service.cache.NodeCacheService;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    @Autowired
    private NodeCacheService nodeCacheService;

    /**
     * Vytvoření chybějících záznamů v arr_cached_node
     */
    @Transactional
    public ResponseEntity<Void> syncNodeCache() {
        nodeCacheService.syncCache();
        return ResponseEntity.ok().build();
    }

}
