package cz.tacr.elza.service.vo;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.SysLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Importní objekt pro přístupový bod.
 *
 * @since 12.07.2018
 */
public class ImportAccessPoint implements IApScope {

    /**
     * Třída.
     */
    private ApScope scope;

    /**
     * Typ.
     */
    private ApType type;

    /**
     * Popis / charakteristika.
     */
    private String description;

    /**
     * Preferované jméno.
     */
    private Name preferredName;

    /**
     * Nepreferovaná jména.
     */
    private List<Name> names;

    @Override
    public Integer getScopeId() {
        return scope == null ? null : scope.getScopeId();
    }

    /**
     * Jméno přístupového bodu.
     */
    public class Name {

        /**
         * Název jména.
         */
        private String name;

        /**
         * Doplněk jména.
         */
        private String complement;

        /**
         * Jazyk jména.
         */
        private SysLanguage language;

        public Name(final String name, final String complement, final SysLanguage language) {
            this.name = name;
            this.complement = complement;
            this.language = language;
        }

        public String getName() {
            return name;
        }

        public String getComplement() {
            return complement;
        }

        public SysLanguage getLanguage() {
            return language;
        }
    }

    public void setScope(final ApScope scope) {
        this.scope = scope;
    }

    public void setType(final ApType type) {
        this.type = type;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setPreferredName(final Name preferredName) {
        this.preferredName = preferredName;
    }

    public void setPreferredName(final String name, final String complement, final SysLanguage language) {
        this.preferredName = new Name(name, complement, language);
    }

    public void addName(final String name, final String complement, final SysLanguage language) {
        if (names == null) {
            names = new ArrayList<>();
        }
        names.add(new Name(name, complement, language));
    }

    public ApScope getScope() {
        return scope;
    }

    public ApType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Name getPreferredName() {
        return preferredName;
    }

    public List<Name> getNames() {
        return names;
    }
}
