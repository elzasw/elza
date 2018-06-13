package cz.tacr.elza.dataexchange.input.context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;

/**
 * Context for single import execution.
 */
public class ImportContext implements ObservableImport {

    private static final Logger logger = LoggerFactory.getLogger(ImportContext.class);

    private final List<ImportPhaseChangeListener> phaseChangeListeners = new LinkedList<>();

    private final Session session;

    private final AccessPointsContext accessPoints;

    private final PartiesContext parties;

    private final InstitutionsContext institutions;

    private final SectionsContext sections;

    private final StaticDataProvider staticData;

    private ImportPhase currentPhase = ImportPhase.INIT;

    public ImportContext(Session session,
                         StaticDataProvider staticData,
                         AccessPointsContext accessPoints,
                         PartiesContext parties,
                         InstitutionsContext institutions,
                         SectionsContext sections) {
        this.session = session;
        this.staticData = staticData;
        this.accessPoints = accessPoints;
        this.parties = parties;
        this.institutions = institutions;
        this.sections = sections;
    }

    public Session getSession() {
        return session;
    }

    public StaticDataProvider getStaticData() {
        return staticData;
    }

    public AccessPointsContext getAccessPoints() {
        return accessPoints;
    }

    public PartiesContext getParties() {
        return parties;
    }

    public InstitutionsContext getInstitutions() {
        return institutions;
    }

    public SectionsContext getSections() {
        return sections;
    }

    public void setCurrentPhase(ImportPhase phase) {
        if (currentPhase == phase) {
            return;
        }
        if (phase.isSubsequent(currentPhase)) {
            throw new IllegalStateException("Next phase is not subsequent");
        }
        List<ImportPhaseChangeListener> listeners = new ArrayList<>(phaseChangeListeners);
        for (ImportPhaseChangeListener l : listeners) {
            boolean unregister = !l.onPhaseChange(currentPhase, phase, this);
            if (unregister) {
                phaseChangeListeners.remove(l);
            }
        }
        logger.info("Import phase changed, previous:" + currentPhase + ", next:" + phase);
        currentPhase = phase;
    }

    @Override
    public void registerPhaseChangeListener(ImportPhaseChangeListener phaseChangeListener) {
        phaseChangeListeners.add(phaseChangeListener);
    }

    public void initSubContexts() {
        accessPoints.init(this);
        parties.init(this);
        institutions.init(this);
        sections.init(this);
    }
}
