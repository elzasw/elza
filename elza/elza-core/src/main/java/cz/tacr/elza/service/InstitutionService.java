package cz.tacr.elza.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.Institution;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * CRUD for {@link ParInstitution} exposed via /api/v1/institution.
 */
@Service
public class InstitutionService {

    @Autowired
    private InstitutionTypeRepository institutionTypeRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ApIndexRepository indexRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private EventNotificationService eventNotificationService;

    public List<ParInstitution> findAll() {
        return institutionRepository.findAllWithFetch();
    }

    public List<ParInstitutionType> findAllTypes() {
        return institutionTypeRepository.findAll();
    }    
    
    /** Resolve institution by numeric id or by {@code internalCode}. */
    public ParInstitution findByIdOrCode(final String idOrCode) {
        Assert.notNull(idOrCode, "Identifier must not be null");
        Integer numericId = tryParseInt(idOrCode);
        ParInstitution found = numericId != null
                ? institutionRepository.findById(numericId).orElse(null)
                : institutionRepository.findByInternalCode(idOrCode);
        if (found == null) {
            throw new ObjectNotFoundException("Instituce neexistuje", BaseCode.ID_NOT_EXIST).setId(idOrCode);
        }
        return found;
    }

    public ParInstitution create(final Institution dto) {
        Assert.notNull(dto, "Institution must not be null");
        Assert.hasText(dto.getInternalCode(), "internalCode must be filled");
        Assert.notNull(dto.getAccessPointId(), "accessPointId must be filled");

        ParInstitutionType type = requireType(dto.getInstitutionTypeId());
        ApAccessPoint ap = accessPointService.getAccessPointInternal(dto.getAccessPointId());

        ParInstitution institution = new ParInstitution();
        institution.setInternalCode(dto.getInternalCode());
        institution.setInstitutionType(type);
        institution.setAccessPoint(ap);
        institution.setName(dto.getName() == null ? displayNameOf(ap) : dto.getName());
        institution.setShortName(dto.getShortName() == null ? shortNameOf(ap) : dto.getShortName());

        ParInstitution saved = institutionRepository.save(institution);
        eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
        return saved;
    }

    /**
     * Updates mutable fields only: {@code internalCode}, {@code shortName},
     * {@code institutionType}. AccessPoint link is immutable.
     */
    public ParInstitution update(final Integer id, final Institution dto) {
        Assert.notNull(dto, "Institution must not be null");
        Assert.hasText(dto.getInternalCode(), "internalCode must be filled");

        ParInstitutionType type = requireType(dto.getInstitutionTypeId());
        ApAccessPoint ap = accessPointService.getAccessPointInternal(dto.getAccessPointId());

        ParInstitution institution = requireById(id);
        institution.setInternalCode(dto.getInternalCode());
        institution.setInstitutionType(type);
        institution.setAccessPoint(ap);
        institution.setName(dto.getName() == null ? displayNameOf(ap) : dto.getName());
        institution.setShortName(dto.getShortName() == null ? shortNameOf(ap) : dto.getShortName());

        ParInstitution saved = institutionRepository.save(institution);
        eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
        return saved;
    }

    public void delete(final Integer id) {
        ParInstitution institution = requireById(id);
        institutionRepository.delete(institution);
        eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
    }

    private String displayNameOf(final ApAccessPoint ap) {
        ApIndex idx = indexRepository.findPreferredPartIndexByAccessPointAndIndexType(ap, GroovyResult.DISPLAY_NAME);
        return idx != null ? idx.getIndexValue() : null;
    }

    private String shortNameOf(final ApAccessPoint ap) {
        ApIndex idx = indexRepository.findPreferredPartIndexByAccessPointAndIndexType(ap, GroovyResult.SHORT_NAME);
        return idx != null ? idx.getIndexValue() : null;
    }

    private ParInstitution requireById(final Integer id) {
        Assert.notNull(id, "id must not be null");
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Instituce neexistuje", BaseCode.ID_NOT_EXIST).setId(id));
    }

    private ParInstitutionType requireType(final Integer institutionTypeId) {
        if (institutionTypeId == null) {
            return null;
        }
        return institutionTypeRepository.findById(institutionTypeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ instituce neexistuje", BaseCode.ID_NOT_EXIST)
                        .setId(institutionTypeId));
    }

    private static Integer tryParseInt(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}