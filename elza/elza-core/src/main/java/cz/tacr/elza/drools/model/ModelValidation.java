package cz.tacr.elza.drools.model;


import java.util.List;

import cz.tacr.elza.drools.model.item.AbstractItem;

/**
 * AccessPoint validation model
 */
public class ModelValidation {

    private Ap ap;

    private GeoModel geoModel;

    private List<ModelPart> modelParts;

    private ApValidationErrors apValidationErrors;

    private List<AbstractItem> items;

    private ExpectedItems expectedItems;

    public ModelValidation(final Ap ap,
                           final GeoModel geoModel,
                           final List<ModelPart> modelParts,
                           final ApValidationErrors apValidationErrors,
                           final List<AbstractItem> items,
                           final ExpectedItems expectedItems) {
        this.ap = ap;
        this.geoModel = geoModel;
        this.modelParts = modelParts;
        this.apValidationErrors = apValidationErrors;
        this.items = items;
        this.expectedItems = expectedItems;
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

    public ExpectedItems getExpectedItems() {
        return expectedItems;
    }
}
