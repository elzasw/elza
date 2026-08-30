package cz.tacr.elza.service.event;

import org.springframework.context.ApplicationEvent;

import cz.tacr.elza.domain.ArrDigitalRepository;

/**
 * Událost změny nebo smazání úložiště digitalizátů.
 * Slouží k přeplánování synchronizace s digitálním archivem.
 */
public class ArrDigitalRepositoryEvent extends ApplicationEvent {

    private final ArrDigitalRepository digitalRepository;

    public ArrDigitalRepositoryEvent(Object source, ArrDigitalRepository digitalRepository) {
        super(source);
        this.digitalRepository = digitalRepository;
    }

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }
}
