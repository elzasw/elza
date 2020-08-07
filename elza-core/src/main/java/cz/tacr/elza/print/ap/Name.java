package cz.tacr.elza.print.ap;

import cz.tacr.elza.domain.SysLanguage;

public class Name {

    private final String name;

    private final String complement;

    private final String fullName;
    
    private final boolean preferred;
    
    private final SysLanguage language;

    private Name(String name, String complement, String fullName, boolean preferred, SysLanguage language) {
        this.name = name;
        this.complement = complement;
        this.fullName = fullName;
        this.preferred = preferred;
        this.language = language;
    }
    
    public String getName() {
        return name;
    }

    public String getComplement() {
        return complement;
    }
    
    public String getFullName() {
        return fullName;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public SysLanguage getLanguage() {
        return language;
    }
}
