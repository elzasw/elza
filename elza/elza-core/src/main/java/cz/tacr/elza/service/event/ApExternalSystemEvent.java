package cz.tacr.elza.service.event;

import org.springframework.context.ApplicationEvent;

import cz.tacr.elza.domain.ApExternalSystem;

/**
 * Událost změny nebo smazání externího systému.
 * Slouží k invalidaci cache v CAM konektorech.
 */
public class ApExternalSystemEvent extends ApplicationEvent {

    private final ApExternalSystem externalSystem;

    public ApExternalSystemEvent(Object source, ApExternalSystem externalSystem) {
        super(source);
        this.externalSystem = externalSystem;
    }

    public ApExternalSystem getExternalSystem() {
        return externalSystem;
    }
}