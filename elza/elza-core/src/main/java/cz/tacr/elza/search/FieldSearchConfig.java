package cz.tacr.elza.search;

public class FieldSearchConfig {

    private String name;

    private Float boost;

    private Float boostExact;

    private Boolean transliterate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getBoost() {
        return boost;
    }

    public void setBoost(Float boost) {
        this.boost = boost;
    }

    public Float getBoostExact() {
        return boostExact;
    }

    public void setBoostExact(Float boostExact) {
        this.boostExact = boostExact;
    }

    public Boolean getTransliterate() {
        return transliterate;
    }

    public void setTransliterate(Boolean transliterate) {
        this.transliterate = transliterate;
    }
}
