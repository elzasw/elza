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

public class PartiesReader implements ExportReader {

    private final ExportContext context;

    private final EntityManager em;

    public PartiesReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.em = initHelper.getEm();
    }

    @Override
    public void read() {
        PartiesOutputStream os = context.getBuilder().openPartiesOutputStream();
        try {
            Set<Integer> exportedApIds = readPartyIds(os);
            readApIds(os, exportedApIds);
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
        Set<Integer> exportedApIds = new HashSet<>();

        PartyInfoLoader loader = PartyInfoLoader.createPartyIdLoader(em, context.getBatchSize(), context.getStaticData());
        for (Integer partyId : context.getPartyIds()) {
            PartyInfoDispatcher dispatcher = new PartyInfoDispatcher(context.getStaticData()) {
                @Override
                protected void onCompleted() {
                    PartyInfo partyInfo = Validate.notNull(getPartyInfo());
                    exportedApIds.add(partyInfo.getParty().getAccessPointId());
                    os.addParty(partyInfo);
                }
            };
            loader.addRequest(partyId, dispatcher);
        }
        loader.flush();

        return exportedApIds;
    }

    private void readApIds(PartiesOutputStream os, Set<Integer> exportedApIds) {
        Set<Integer> apIds = SetUtils.difference(context.getPartyApIds(), exportedApIds);

        PartyInfoLoader loader = PartyInfoLoader.createAPIdLoader(em, context.getBatchSize(), context.getStaticData());
        for (Integer apId : apIds) {
            PartyInfoDispatcher dispatcher = new PartyInfoDispatcher(context.getStaticData()) {
                @Override
                protected void onCompleted() {
                    PartyInfo partyInfo = Validate.notNull(getPartyInfo());
                    os.addParty(partyInfo);
                }
            };
            loader.addRequest(apId, dispatcher);
        }
        loader.flush();
    }
}
