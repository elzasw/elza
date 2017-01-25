package cz.tacr.elza.interpi.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.interpi.ws.wo.EntitaTyp;

/**
 * Třída pro získání uživatelské session během importu z INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 1. 2017
 */
@Configuration
@Service
public class InterpiSessionHolder {

    /**
     * @return vrací session uživatele
     */
    @Bean
    @Scope("session")
    public InterpiEntitySession getInterpiEntitySession() {
        return new InterpiEntitySession();
    }


    /**
     * Session uživatele s načtenou entitou z INTERPI.
     */
    @Component
    static class InterpiEntitySession {

        private EntitaTyp entitaTyp;

        private Map<String, EntitaTyp> relatedEntities;

        public void addRelatedEntity(final String id, final EntitaTyp entitaTyp) {
            Assert.notNull(id);
            Assert.notNull(entitaTyp);

            if (relatedEntities == null) {
                relatedEntities = new HashMap<>();
            }

            relatedEntities.put(id, entitaTyp);
        }

        public EntitaTyp getRelatedEntity(final String id) {
            Assert.notNull(id);

            if (relatedEntities == null) {
                return null;
            }

            return relatedEntities.get(id);
        }

        public void clear() {
            entitaTyp = null;
            relatedEntities = null;
        }

        public EntitaTyp getEntitaTyp() {
            return entitaTyp;
        }

        public void setEntitaTyp(final EntitaTyp entitaTyp) {
            this.entitaTyp = entitaTyp;
        }
    }
}
