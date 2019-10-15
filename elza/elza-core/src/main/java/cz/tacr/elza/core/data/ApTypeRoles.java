package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.common.FactoryUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParRegistryRole;

public class ApTypeRoles {

    private final ApType type;
    
    private List<ParRegistryRole> roles = new ArrayList<>();
    
    ApTypeRoles(ApType type) {
       this.type = type;
    }
    
    public ApType getType() {
        return type;
    }
    
    public List<ParRegistryRole> getRoles() {
        return roles;
    }
    
    public List<Integer> getRoleIds() {
        return FactoryUtils.transformList(roles, ParRegistryRole::getRegistryRoleId);
    }

    void addRole(ParRegistryRole role) {
        Validate.notNull(role);
        roles.add(role);
    }

    void sealUp() {
        // switch to unmodifiable collections
        roles = Collections.unmodifiableList(roles);
    }
}
