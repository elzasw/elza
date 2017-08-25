package cz.tacr.elza.deimport.context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.aps.context.AccessPointsContext;
import cz.tacr.elza.deimport.institutions.context.InstitutionsContext;
import cz.tacr.elza.deimport.parties.context.PartiesContext;
import cz.tacr.elza.deimport.sections.context.SectionsContext;

/**
 * Context for single import execution.
 */
public class ImportContext implements ObservableImport {

    private static final Logger LOG = LoggerFactory.getLogger(ImportContext.class);

    private final List<ImportPhaseChangeListener> phaseChangeListeners = new LinkedList<>();

    private final DatatypeFactory datatypeFactory = createDatatypeFactory();

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
        initAllContexts();
    }

    public DatatypeFactory getDatatypeFactory() {
        return datatypeFactory;
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
        LOG.info("Import phase changed, previous:" + currentPhase + ", next:" + phase);
        currentPhase = phase;
    }

    @Override
    public void registerPhaseChangeListener(ImportPhaseChangeListener phaseChangeListener) {
        phaseChangeListeners.add(phaseChangeListener);
    }

    private void initAllContexts() {
        accessPoints.init(this);
        parties.init(this);
        institutions.init(this);
        sections.init(this);
    }

    private static DatatypeFactory createDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
