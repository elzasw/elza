package cz.tacr.elza.deimport.context;

import java.io.Serializable;

import javax.xml.ws.Holder;

public abstract class StatefulIdHolder extends IdHolder {

    private final Holder<EntityState> stateHolder;

    public StatefulIdHolder() {
        stateHolder = new Holder<>();
    }

    /**
     * Instance will share same state which can be modified by this and source holder.
     */
    public StatefulIdHolder(StatefulIdHolder sourceHolder) {
        stateHolder = sourceHolder.stateHolder;
    }

    public EntityState getState() {
        return stateHolder.value;
    }

    protected void init(Serializable id, EntityState state) {
        init(id);
        stateHolder.value = state;
    }
}
