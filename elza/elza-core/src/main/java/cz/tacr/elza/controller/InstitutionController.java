package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.mapper.InstitutionMapper;
import cz.tacr.elza.controller.vo.Institution;
import cz.tacr.elza.controller.vo.InstitutionType;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.service.InstitutionService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class InstitutionController implements InstitutionApi {

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Override
    @Transactional
    @AuthMethod(permission = {})
    public ResponseEntity<List<InstitutionType>> instGetTypes() {
        return ResponseEntity.ok(institutionMapper.toTypeDtoList(institutionService.findAllTypes()));
    }    

    /**
     * GET /institution/
     * Get list of Institutions
     *
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {})
    public ResponseEntity<List<Institution>> instGetAll() {
        List<ParInstitution> all = institutionService.findAll();
        return ResponseEntity.ok(institutionMapper.toDtoList(all));
    }

    /**
     * GET /institution/{id}
     * Get institution by id
     *
     * @param id institution id or internalCode (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {})
    public ResponseEntity<Institution> instGetById(final String id) {
        ParInstitution found = institutionService.findByIdOrCode(id);
        return ResponseEntity.ok(institutionMapper.toDto(found));
    }

    /**
     * POST /institution/
     * Create institution
     *
     * @param institution item of institution (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Institution> instCreate(final Institution institution) {
    	ParInstitution created = institutionService.create(institution);
        return ResponseEntity.ok(institutionMapper.toDto(created));
    }

    /**
     * PUT /institution/{id}
     * Update institution
     *
     * @param id institution id (required)
     * @param institution item of institurion (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Institution> instUpdate(final Integer id, final Institution institution) {
    	ParInstitution updated = institutionService.update(id, institution);
        return ResponseEntity.ok(institutionMapper.toDto(updated));
    }

    /**
     * DELETE /institution/{id}
     * Delete institution
     *
     * @param id institution id (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Void> instDelete(final Integer id) {
        institutionService.delete(id);
        return ResponseEntity.ok().build();
    }
}