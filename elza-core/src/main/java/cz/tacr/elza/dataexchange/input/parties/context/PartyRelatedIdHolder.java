package cz.tacr.elza.dataexchange.input.parties.context;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;

public class PartyRelatedIdHolder<E> extends EntityIdHolder<E> {

    private final PartyInfo partyInfo;

    public PartyRelatedIdHolder(Class<E> entityClass, PartyInfo partyInfo) {
        super(entityClass);
        this.partyInfo = partyInfo;
    }

    public PartyRelatedIdHolder(Class<E> entityClass, PartyRelatedIdHolder<?> sourceHolder) {
        this(entityClass, sourceHolder.partyInfo);
    }

    public PartyInfo getPartyInfo() {
        return partyInfo;
    }

    @Override
    protected boolean isReferenceInitOnDemand() {
        return true;
    }
}
