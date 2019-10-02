package cz.tacr.elza.controller.vo;

import java.util.List;

public class LogVO {

    private List<String> lines;

    private Integer lineCount;

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }
}
