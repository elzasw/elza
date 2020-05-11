package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.ObjectIdHolder;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Context for single import execution.
 */
public class ImportContext implements ObservableImport {

    private static final Logger logger = LoggerFactory.getLogger(ImportContext.class);

    private final List<ImportPhaseChangeListener> phaseChangeListeners = new LinkedList<>();

    private final Session session;

    private final AccessPointsContext accessPoints;

    private final InstitutionsContext institutions;

    private final PartsContext parts;

    private final SectionsContext sections;

    private final StaticDataProvider staticData;

    private final StorageManager storageManager;

    private ImportPhase currentPhase = ImportPhase.INIT;



    public ImportContext(Session session,
            StaticDataProvider staticData,
            AccessPointsContext accessPoints,
            InstitutionsContext institutions,
            SectionsContext sections,
            PartsContext parts,
            StorageManager storageManager) {
        this.session = session;
        this.staticData = staticData;
        this.accessPoints = accessPoints;
        this.institutions = institutions;
        this.sections = sections;
        this.parts = parts;
        this.storageManager = storageManager;
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

    public InstitutionsContext getInstitutions() {
        return institutions;
    }

    public SectionsContext getSections() {
        return sections;
    }

    public PartsContext getParts() { return parts; }

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

    public void init(Collection<ImportPhaseChangeListener> listeners) {
        accessPoints.init(this);
        institutions.init(this);
        sections.init(this);
        parts.init(this);

        if (listeners != null) {
        	listeners.forEach(phaseChangeListeners::add);
        }
    }

	public void finish() {
		setCurrentPhase(ImportPhase.FINISHED);
		storageManager.clear();
	}
}
