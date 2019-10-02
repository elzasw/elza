package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class NameComplementLoader extends AbstractEntityLoader<ParPartyNameComplement> {

    public NameComplementLoader(EntityManager em, int batchSize) {
        super(ParPartyNameComplement.class, ParPartyNameComplement.PARTY_NAME_FK, em, batchSize);
    }
}