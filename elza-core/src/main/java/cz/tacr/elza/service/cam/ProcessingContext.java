package cz.tacr.elza.service.cam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;

/**
 * Context for processing access points from CAM
 *
 */
public class ProcessingContext {

    private ApScope scope;
    private ApExternalSystem apExternalSystem;

    private Map<String, ApBinding> bindings = new HashMap<>();

    public ProcessingContext(final ApScope scope,
                             final ApExternalSystem apExternalSystem) {
        this.scope = scope;
        this.apExternalSystem = apExternalSystem;
    }

    public ApScope getScope() {
        return scope;
    }

    public ApExternalSystem getApExternalSystem() {
        return apExternalSystem;
    }

    /**
     * Add bindings to the context
     * 
     * @param bindingList
     */
    public void addBindings(List<ApBinding> bindingList) {
        bindingList.forEach(b -> bindings.put(b.getValue(), b));
    }

    public ApBinding getBindingByValue(String bindingValue) {
        return bindings.get(bindingValue);
    }

    public void addBinding(ApBinding binding) {
        bindings.put(binding.getValue(), binding);
    }

}
