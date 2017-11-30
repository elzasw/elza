package cz.tacr.elza.dataexchange.output.parties;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.PartiesOutputStream;
import cz.tacr.elza.domain.ParParty;

public class PartiesReader implements ExportReader {

    private final ExportContext context;

    private final EntityManager em;

    public PartiesReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.em = initHelper.getEntityManager();
    }

    @Override
    public void read() {
        PartiesOutputStream os = context.getBuilder().openPartiesOutputStream();
        try {
            Set<Integer> exportedAPIds = readPartyIds(os);
            readAPIds(os, exportedAPIds);
            os.processed();
        } finally {
            os.close();
        }
    }

    /**
     * Reads all parties by party id specified in context.
     *
     * @return Exported access point ids.
     */
    private Set<Integer> readPartyIds(PartiesOutputStream os) {
        Set<Integer> exportedAPIds = new HashSet<>();

        PartyLoader loader = PartyLoader.createPartyIdLoader(em, context.getBatchSize(), context.getStaticData());
        for (Integer partyId : context.getPartyIds()) {
            PartyDispatcher dispatcher = new PartyDispatcher() {
                @Override
                protected void onCompleted() {
                    ParParty party = Validate.notNull(getParty());
                    exportedAPIds.add(party.getRecord().getRecordId());
                    os.addParty(party);
                }
            };
            loader.addRequest(partyId, dispatcher);
        }
        loader.flush();

        return exportedAPIds;
    }

    private void readAPIds(PartiesOutputStream os, Set<Integer> exportedAPIds) {
        Set<Integer> apIds = SetUtils.difference(context.getPartyAPIds(), exportedAPIds);

        PartyLoader loader = PartyLoader.createAPIdLoader(em, context.getBatchSize(), context.getStaticData());
        for (Integer apId : apIds) {
            PartyDispatcher dispatcher = new PartyDispatcher() {
                @Override
                protected void onCompleted() {
                    os.addParty(Validate.notNull(getParty()));
                }
            };
            loader.addRequest(apId, dispatcher);
        }
        loader.flush();
    }
}
