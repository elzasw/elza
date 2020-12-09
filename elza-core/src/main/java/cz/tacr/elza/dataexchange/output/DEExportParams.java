package cz.tacr.elza.dataexchange.output;

import java.util.Collection;

import cz.tacr.elza.dataexchange.output.sections.LevelInfoListener;

/**
 * Parameters for data-exchange export.
 */
public class DEExportParams {

    private Collection<Integer> apIds;

    private Collection<FundSections> fundsSections;

    public Collection<Integer> getApIds() {
        return apIds;
    }

    public void setApIds(Collection<Integer> apIds) {
        this.apIds = apIds;
    }

    public Collection<FundSections> getFundsSections() {
        return fundsSections;
    }

    public void setFundsSections(Collection<FundSections> fundsSections) {
        this.fundsSections = fundsSections;
    }

    /**
     * Specifies multiple sections for fund version. Overlap is allowed.
     */
    public static class FundSections {

        private int fundVersionId;

        // if true sections are exported as single section with parent path up to root
        private boolean mergeSections;

        private Collection<Integer> rootNodeIds;

        private LevelInfoListener levelInfoListener;

        public int getFundVersionId() {
            return fundVersionId;
        }

        public void setFundVersionId(int fundVersionId) {
            this.fundVersionId = fundVersionId;
        }

        public boolean isMergeSections() {
            return mergeSections;
        }

        public void setMergeSections(boolean mergeSections) {
            this.mergeSections = mergeSections;
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

        public LevelInfoListener getLevelInfoListener() {
            return levelInfoListener;
        }

        public void setLevelInfoListener(LevelInfoListener levelInfoListener) {
            this.levelInfoListener = levelInfoListener;
        }
    }
}
