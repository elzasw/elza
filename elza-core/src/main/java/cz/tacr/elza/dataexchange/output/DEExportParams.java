package cz.tacr.elza.dataexchange.output;

import java.util.Collection;

/**
 * Parameters for data-exchange export.
 */
public class DEExportParams {

    private Collection<Integer> apIds;

    private Collection<Integer> partyIds;

    private Collection<FundParams> fundsParams;

    public Collection<Integer> getApIds() {
        return apIds;
    }

    public void setApIds(Collection<Integer> apIds) {
        this.apIds = apIds;
    }

    public Collection<Integer> getPartyIds() {
        return partyIds;
    }

    public void setPartyIds(Collection<Integer> partyIds) {
        this.partyIds = partyIds;
    }

    public Collection<FundParams> getFundsParams() {
        return fundsParams;
    }

    public void setFundsParams(Collection<FundParams> fundsParams) {
        this.fundsParams = fundsParams;
    }

    /**
     * Specifies multiple sections for single fund version. Overlap is allowed.
     */
    public static class FundParams {

        private int fundVersionId;

        private Collection<Integer> rootNodeIds;

        public int getFundVersionId() {
            return fundVersionId;
        }

        public void setFundVersionId(int fundVersionId) {
            this.fundVersionId = fundVersionId;
        }

        /**
         * Return root node ids. When null or empty whole fund is exported.
         */
        public Collection<Integer> getRootNodeIds() {
            return rootNodeIds;
        }

        public void setRootNodeIds(Collection<Integer> rootNodeIds) {
            this.rootNodeIds = rootNodeIds;
        }
    }
}
