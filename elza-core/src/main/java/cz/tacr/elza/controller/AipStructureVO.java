package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.DaDaoFileFolderVO;
import cz.tacr.elza.controller.vo.DaDaoFileVO;

import java.util.List;

public class AipStructureVO {
    private DaDaoFileFolderVO representation;
    private DaDaoFileFolderVO logical;
    private List<DaDaoFileVO> metadata;

    public DaDaoFileFolderVO getRepresentation() {
        return representation;
    }

    public void setRepresentation(DaDaoFileFolderVO representation) {
        this.representation = representation;
    }

    public DaDaoFileFolderVO getLogical() {
        return logical;
    }

    public void setLogical(DaDaoFileFolderVO logical) {
        this.logical = logical;
    }

    public List<DaDaoFileVO> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<DaDaoFileVO> metadata) {
        this.metadata = metadata;
    }
}
