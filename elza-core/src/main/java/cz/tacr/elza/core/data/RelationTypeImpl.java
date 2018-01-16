package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;

public class RelationTypeImpl implements RelationType {

    private final ParRelationType type;

    private List<ParRelationRoleType> roleTypes;

    private Map<Integer, ParRelationRoleType> roleTypeIdMap;

    private Map<String, ParRelationRoleType> roleTypeCodeMap;

    public RelationTypeImpl(ParRelationType type) {
        this.type = Validate.notNull(type);
        this.roleTypes = new ArrayList<>();
    }

    @Override
    public Integer getId() {
        return type.getRelationTypeId();
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @Override
    public String getCode() {
        return type.getCode();
    }

    @Override
    public ParRelationType getEntity() {
        return type;
    }

    @Override
    public List<ParRelationRoleType> getRoleTypes() {
        return roleTypes;
    }

    @Override
    public ParRelationRoleType getRoleTypeById(Integer id) {
        Validate.notNull(id);
        return roleTypeIdMap.get(id);
    }

    @Override
    public ParRelationRoleType getRoleTypeByCode(String code) {
        Validate.notEmpty(code);
        return roleTypeCodeMap.get(code);
    }

    public void addRoleType(ParRelationRoleType roleType) {
        Validate.notNull(roleType);
        roleTypes.add(roleType);
    }

    public RelationTypeImpl sealUp() {
        // update fields
        this.roleTypeIdMap = StaticDataProvider.createLookup(roleTypes, ParRelationRoleType::getRoleTypeId);
        this.roleTypeCodeMap = StaticDataProvider.createLookup(roleTypes, ParRelationRoleType::getCode);

        // switch to unmodifiable collections
        roleTypes = Collections.unmodifiableList(roleTypes);

        return this;
    }
}
