package cz.tacr.elza.print.ap;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApName;
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
    
    public static Name newInstance(ApName name, StaticDataProvider staticData) {
        SysLanguage lang = null;
        if (name.getLanguageId() != null) {
            lang = staticData.getSysLanguageById(name.getLanguageId());
        }
        return new Name(name.getName(), name.getComplement(), name.getFullName(), name.isPreferredName(), lang);
    }
}
