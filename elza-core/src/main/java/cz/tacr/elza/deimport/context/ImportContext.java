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
public class ImportContext implements ImportObserver {

    private static final Logger LOG = LoggerFactory.getLogger(ImportContext.class);

    /**
     * Defines import phase. Ordinal value must be preserved.
     */
    public enum ImportPhase {
        INIT, ACCESS_POINTS, PARTIES, INSTITUTIONS, RELATIONS, SECTIONS, FINISHED;

        /**
         * @return True when specified phase is subsequent.
         */
        public boolean isSubsequent(ImportPhase phase) {
            return ordinal() < phase.ordinal();
        }
    }

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
                         AccessPointsContext accessPoints,
                         PartiesContext parties,
                         InstitutionsContext institutions,
                         SectionsContext sections,
                         StaticDataProvider staticData) {
        this.session = session;
        this.accessPoints = accessPoints;
        this.parties = parties;
        this.institutions = institutions;
        this.sections = sections;
        this.staticData = staticData;

        // init all contexts
        accessPoints.init(this);
        parties.init(this);
        institutions.init(this);
        sections.init(this);
    }

    public DatatypeFactory getDatatypeFactory() {
        return datatypeFactory;
    }

    public Session getSession() {
        return session;
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

    public StaticDataProvider getStaticData() {
        return staticData;
    }

    public void setCurrentPhase(ImportPhase nextPhase) {
        if (currentPhase == nextPhase) {
            return;
        }
        if (nextPhase.isSubsequent(currentPhase)) {
            throw new IllegalStateException("Next phase is not subsequent");
        }
        List<ImportPhaseChangeListener> listeners = new ArrayList<>(phaseChangeListeners);
        for (ImportPhaseChangeListener l : listeners) {
            boolean unregister = !l.onPhaseChange(currentPhase, nextPhase, this);
            if (unregister) {
                phaseChangeListeners.remove(l);
            }
        }
        LOG.info("Import phase changed, previous:" + currentPhase + ", next:" + nextPhase);
        currentPhase = nextPhase;
    }

    @Override
    public void registerPhaseChangeListener(ImportPhaseChangeListener phaseChangeListener) {
        phaseChangeListeners.add(phaseChangeListener);
    }

    private static DatatypeFactory createDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
