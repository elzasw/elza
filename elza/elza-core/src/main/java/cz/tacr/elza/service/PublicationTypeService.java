package cz.tacr.elza.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.vo.ConnectionType;
import cz.tacr.elza.controller.vo.PublicationType;
import cz.tacr.elza.domain.ArrExportType;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ConflictException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ExportFilterRepository;
import cz.tacr.elza.repository.ExportRepository;
import cz.tacr.elza.repository.ExportTypeRepository;

/**
 * Manages publication types ({@code arr_export_type}) — the target-system
 * configurations consumed by the public {@code /publications} API and by the
 * fund-scoped publication endpoints. Handles CRUD with duplicate-code and
 * referenced-by-export conflict detection.
 */
@Service
public class PublicationTypeService {

    /** Default retention applied when the API request does not carry a value. */
    public static final int DEFAULT_RETENTION_COUNT = 5;

    @Autowired
    private ExportTypeRepository exportTypeRepository;

    @Autowired
    private ExportFilterRepository exportFilterRepository;

    @Autowired
    private ExportRepository exportRepository;

    @Transactional(readOnly = true)
    public List<ArrExportType> listAll() {
        return exportTypeRepository.findAll(Sort.by(ArrExportType.FIELD_NAME).ascending());
    }

    @Transactional
    public ArrExportType create(final PublicationType vo) {
        if (exportTypeRepository.findByCode(vo.getCode()).isPresent()) {
            throw new ConflictException("Publication type with code already exists",
                    BaseCode.PROPERTY_IS_INVALID)
                    .set("property", "code")
                    .set("code", vo.getCode());
        }
        ArrExportType entity = createEntity(vo, null);
        return exportTypeRepository.save(entity);
    }

    @Transactional
    public ArrExportType update(final Integer id, final PublicationType vo) {
        ArrExportType entity = exportTypeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Publication type not found", BaseCode.ID_NOT_EXIST).setId(id));

        if (!entity.getCode().equals(vo.getCode())) {
            Optional<ArrExportType> conflicting = exportTypeRepository.findByCode(vo.getCode());
            if (conflicting.isPresent() && !conflicting.get().getExportTypeId().equals(id)) {
                throw new ConflictException("Publication type with code already exists",
                        BaseCode.PROPERTY_IS_INVALID)
                        .set("property", "code")
                        .set("code", vo.getCode());
            }
        }

        entity = createEntity(vo, entity);
        return exportTypeRepository.save(entity);
    }

    @Transactional
    public void delete(final Integer id) {
        ArrExportType entity = exportTypeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Publication type not found", BaseCode.ID_NOT_EXIST).setId(id));

        if (exportRepository.existsByExportTypeExportTypeId(id)) {
            throw new ConflictException("Publication type is referenced by existing publications", BaseCode.DB_INTEGRITY_PROBLEM).set("id", id);
        }
        exportTypeRepository.delete(entity);
    }

    public PublicationType toVO(final ArrExportType entity) {
        PublicationType vo = new PublicationType();
        vo.setId(entity.getExportTypeId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setActive(entity.getActive());
        vo.setRetentionCount(entity.getRetentionCount());
        vo.setAllowPermExport(entity.getAllowPermExport());
        vo.setAllowPermPublication(entity.getAllowPermPublication());
        vo.setConnectionType(ConnectionType.fromValue(entity.getConnectionType().name()));
        if (entity.getExportFilter() != null) {
            vo.setExportFilterCode(entity.getExportFilter().getCode());
        }
        return vo;
    }

    private ArrExportType createEntity(final PublicationType vo, ArrExportType entity) {
        if (vo.getName() == null || vo.getName().isBlank()) {
            throw new BusinessException("Publication type name must be set", BaseCode.PROPERTY_NOT_EXIST).set("property", "name");
        }
        if (vo.getCode() == null || vo.getCode().isBlank()) {
            throw new BusinessException("Publication type code must be set", BaseCode.PROPERTY_NOT_EXIST).set("property", "code");
        }
        if (vo.getConnectionType() == null) {
            throw new BusinessException("Publication type connection type must be set", BaseCode.PROPERTY_NOT_EXIST).set("property", "connectionType");
        }
        if (entity == null) {
        	entity = new ArrExportType();
        }
        entity.setName(vo.getName());
        entity.setCode(vo.getCode());
        entity.setActive(vo.getActive() == null ? Boolean.TRUE : vo.getActive());
        entity.setRetentionCount(vo.getRetentionCount() == null ? DEFAULT_RETENTION_COUNT : vo.getRetentionCount());
        entity.setAllowPermExport(Boolean.TRUE.equals(vo.getAllowPermExport()));
        entity.setAllowPermPublication(Boolean.TRUE.equals(vo.getAllowPermPublication()));
        entity.setConnectionType(cz.tacr.elza.domain.ConnectionType.valueOf(vo.getConnectionType().getValue()));
        entity.setExportFilter(resolveExportFilter(vo.getExportFilterCode()));
        return entity;
    }

    private RulExportFilter resolveExportFilter(final String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        RulExportFilter filter = exportFilterRepository.findByCode(code);
        if (filter == null) {
            throw new ObjectNotFoundException("Export filter not found",
                    BaseCode.ID_NOT_EXIST).set("exportFilterCode", code);
        }
        return filter;
    }
}
