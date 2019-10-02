package cz.tacr.elza.service.party;

import cz.tacr.elza.domain.SysLanguage;

public class ApConvName {

    private String name;

    private String complement;

    private SysLanguage language;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public SysLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SysLanguage language) {
        this.language = language;
    }
}
