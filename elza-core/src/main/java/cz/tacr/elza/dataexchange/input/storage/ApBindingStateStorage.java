package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.aps.context.ApExternalIdWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.repository.ApBindingRepository;

/**
 * Storage of AP binding states (external ids).
 *
 * Binding state references {@link ApBinding} which is not cascaded. Binding is a shared
 * entity - single (value, external system) pair must exist only once in DB. Before states
 * are saved all their bindings have to be resolved: either paired with already existing
 * binding or persisted as a new one.
 */
public class ApBindingStateStorage extends EntityStorage<ApExternalIdWrapper> {

    private final ApBindingRepository bindingRepository;

    public ApBindingStateStorage(Session session, StoredEntityCallback persistEntityListener,
                                 ImportInitHelper initHelper) {
        super(session, persistEntityListener);
        this.bindingRepository = initHelper.getBindingRepository();
    }

    @Override
    public void store(Collection<ApExternalIdWrapper> eidws) {
        resolveBindings(eidws);
        super.store(eidws);
    }

    /**
     * Replaces transient bindings by already existing ones, remaining bindings are persisted.
     */
    private void resolveBindings(Collection<ApExternalIdWrapper> eidws) {
        // group states by external system, transient binding cannot be used as map key
        Map<Integer, BindingGroup> groups = new HashMap<>();
        for (ApExternalIdWrapper eidw : eidws) {
            if (eidw.getSaveMethod() == SaveMethod.IGNORE) {
                continue;
            }
            ApBinding binding = eidw.getEntity().getBinding();
            if (binding == null || binding.getBindingId() != null) {
                continue; // no binding or already stored
            }
            ApExternalSystem extSystem = binding.getApExternalSystem();
            groups.computeIfAbsent(extSystem.getExternalSystemId(), id -> new BindingGroup(extSystem))
                    .add(eidw.getEntity());
        }
        for (BindingGroup group : groups.values()) {
            group.resolve();
        }
    }

    private class BindingGroup {

        private final ApExternalSystem extSystem;

        private final Map<String, List<ApBindingState>> statesByValue = new HashMap<>();

        public BindingGroup(ApExternalSystem extSystem) {
            this.extSystem = extSystem;
        }

        public void add(ApBindingState state) {
            statesByValue.computeIfAbsent(state.getBinding().getValue(), v -> new ArrayList<>()).add(state);
        }

        public void resolve() {
            // pair with existing bindings
            List<ApBinding> current = bindingRepository.findByValuesAndExternalSystem(statesByValue.keySet(),
                                                                                      extSystem);
            for (ApBinding binding : current) {
                setBinding(statesByValue.remove(binding.getValue()), binding);
            }
            // create remaining bindings, all states with same value share single binding
            for (List<ApBindingState> states : statesByValue.values()) {
                ApBinding binding = states.get(0).getBinding();
                session.persist(binding);
                setBinding(states, binding);
            }
        }

        private void setBinding(List<ApBindingState> states, ApBinding binding) {
            if (states == null) {
                return;
            }
            for (ApBindingState state : states) {
                state.setBinding(binding);
            }
        }
    }
}
