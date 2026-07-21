package cz.tacr.elza.controller.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cz.tacr.elza.controller.vo.Institution;
import cz.tacr.elza.controller.vo.InstitutionType;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;

@Component
public class InstitutionMapper {

    public Institution toDto(final ParInstitution src) {
        if (src == null) {
            return null;
        }
        Institution dto = new Institution(
                src.getInstitutionId(),
                src.getInternalCode(),
                src.getAccessPointId());
        dto.setName(src.getName());
        dto.setShortName(src.getShortName());
        dto.setInstitutionTypeId(src.getInstitutionTypeId());
        return dto;
    }

    public List<Institution> toDtoList(final List<ParInstitution> src) {
        if (src == null || src.isEmpty()) {
            return new ArrayList<>();
        }
        return src.stream().map(this::toDto).collect(Collectors.toList());
    }

    public InstitutionType toTypeDto(final ParInstitutionType src) {
        if (src == null) {
            return null;
        }
        // packageId not stored on ParInstitutionType yet → stays null
        return new InstitutionType(
                src.getInstitutionTypeId(),
                src.getCode(),
                src.getName());
    }

    public List<InstitutionType> toTypeDtoList(final List<ParInstitutionType> src) {
        if (src == null || src.isEmpty()) {
            return new ArrayList<>();
        }
        return src.stream().map(this::toTypeDto).collect(Collectors.toList());
    }
}