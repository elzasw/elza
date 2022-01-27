package cz.tacr.elza.drools.model;

public class GeoModel {

    private String parentGeoType;
    private String country;
    private boolean extinct;

    public GeoModel(final String parentGeoType, final String country, final boolean extinct) {
        this.parentGeoType = parentGeoType;
        this.country = country;
        this.extinct = extinct;
    }

    public String getParentGeoType() {
        return parentGeoType;
    }

    public void setParentGeoType(String parentGeoType) {
        this.parentGeoType = parentGeoType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isExtinct() {
        return extinct;
    }

    public void setExtinct(boolean extinct) {
        this.extinct = extinct;
    }
}
