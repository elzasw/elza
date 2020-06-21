package cz.tacr.elza.drools.model;


import cz.tacr.elza.drools.model.item.AbstractItem;

import java.util.List;

public class ModelValidation {

    private Ap ap;

    private GeoModel geoModel;

    private List<ModelPart> modelParts;

    private ApValidationErrors apValidationErrors;

    private List<AbstractItem> items;

    public ModelValidation(Ap ap, GeoModel geoModel, List<ModelPart> modelParts, ApValidationErrors apValidationErrors, List<AbstractItem> items) {
        this.ap = ap;
        this.geoModel = geoModel;
        this.modelParts = modelParts;
        this.apValidationErrors = apValidationErrors;
        this.items = items;
    }

    public Ap getAp() {
        return ap;
    }

    public void setAp(Ap ap) {
        this.ap = ap;
    }

    public GeoModel getGeoModel() {
        return geoModel;
    }

    public void setGeoModel(GeoModel geoModel) {
        this.geoModel = geoModel;
    }

    public List<ModelPart> getModelParts() {
        return modelParts;
    }

    public void setModelParts(List<ModelPart> modelParts) {
        this.modelParts = modelParts;
    }

    public ApValidationErrors getApValidationErrors() {
        return apValidationErrors;
    }

    public void setApValidationErrors(ApValidationErrors apValidationErrors) {
        this.apValidationErrors = apValidationErrors;
    }

    public List<AbstractItem> getItems() {
        return items;
    }

    public void setItems(List<AbstractItem> items) {
        this.items = items;
    }
}
