package cz.tacr.elza.drools.model;

public class GeoModel {

    private String parentGeoType;
    private String country;

    public GeoModel(final String parentGeoType, final String country) {
        this.parentGeoType = parentGeoType;
        this.country = country;
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
}
