package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class ArchiveEntityResultListVO {

    private Integer total;

    private List<ArchiveEntityVO> data = new ArrayList<>();

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<ArchiveEntityVO> getData() {
        return data;
    }

    public void setData(List<ArchiveEntityVO> data) {
        this.data = data;
    }
}
