package cz.tacr.elza.core.data;

import java.util.List;

import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;

/**
 * In-memory representation of {@link ParRelationType}.
 */
public interface RelationType {

    Integer getId();

    String getName();

    String getCode();

    ParRelationType getEntity();

    List<ParRelationRoleType> getRoleTypes();

    ParRelationRoleType getRoleTypeById(Integer id);

    ParRelationRoleType getRoleTypeByCode(String code);
}
