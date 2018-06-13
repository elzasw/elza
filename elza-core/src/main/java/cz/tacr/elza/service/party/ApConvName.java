package cz.tacr.elza.service.party;

import cz.tacr.elza.domain.ApNameType;

public class ApConvName {

    private String name;

    private String complement;

    private String language;

    private ApNameType type;
    
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public ApNameType getType() {
        return type;
    }

    public void setType(ApNameType type) {
        this.type = type;
    }
}
