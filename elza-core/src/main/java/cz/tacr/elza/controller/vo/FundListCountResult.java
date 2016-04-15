package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * Objekt obsahující seznam nalezených položek a počet všech položek v db.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public class FundListCountResult {

    private List<ArrFundVO> list;
    private int count;

    public FundListCountResult() {
    }

    public FundListCountResult(final List<ArrFundVO> list, final int count) {
        this.list = list;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public List<ArrFundVO> getList() {
        return list;
    }

    public void setList(final List<ArrFundVO> list) {
        this.list = list;
    }
}
