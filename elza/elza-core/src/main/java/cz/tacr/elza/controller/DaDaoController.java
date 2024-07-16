package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.DaDaoFileFolderVO;
import cz.tacr.elza.controller.vo.DaDaoVO;
import cz.tacr.elza.service.da.DaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dao")
public class DaDaoController {
    @Autowired
    private DaService daService;

    @RequestMapping(value = "/{aipId}", method = RequestMethod.GET)
    public
    DaDaoFileFolderVO getDaDaoByAip(@PathVariable("aipId") final Integer aipId) {
        return daService.findByAipIdAndTypeAndDeleteChangeIsNull(aipId);
    }
}
