package cz.tacr.elza.dataexchange.output.filters;

import java.util.List;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.StaticDataProvider;

public class AccessRestrictConfig implements ExportFilterConfig {

    private List<Def> defs;

    public List<Def> getDefs() {
        return defs;
    }

    public void setDefs(List<Def> defs) {
        this.defs = defs;
    }

    public static class Def {
        private When when;
        private Result result;

        public When getWhen() {
            return when;
        }

        public void setWhen(When when) {
            this.when = when;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }
    }

    public static class When {
        private String structItemType;
        private String itemType;
        private String itemSpec;

        public String getStructItemType() {
            return structItemType;
        }

        public void setStructItemType(String structItemType) {
            this.structItemType = structItemType;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public String getItemSpec() {
            return itemSpec;
        }

        public void setItemSpec(String itemSpec) {
            this.itemSpec = itemSpec;
        }
    }

    public static class Result {
        private Boolean hiddenItem;

        public Boolean getHiddenItem() {
            return hiddenItem;
        }

        public void setHiddenItem(Boolean hiddenItem) {
            this.hiddenItem = hiddenItem;
        }
    }

    @Override
    public ExportFilter createFilter(final EntityManager em, final StaticDataProvider sdp) {
        return new AccessRestrictFilter(em, sdp, this);
    }
}
