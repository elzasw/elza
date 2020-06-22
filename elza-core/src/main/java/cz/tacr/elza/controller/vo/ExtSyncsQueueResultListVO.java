package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class ExtSyncsQueueResultListVO {

    private Integer total = 0;

    private List<ExtSyncsQueueItemVO> data = new ArrayList<>();

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<ExtSyncsQueueItemVO> getData() {
        return data;
    }

    public void setData(List<ExtSyncsQueueItemVO> data) {
        this.data = data;
    }
}
