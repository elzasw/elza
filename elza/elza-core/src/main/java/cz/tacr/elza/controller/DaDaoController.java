package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.DaDaoVO;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.service.da.DaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dao")
public class DaDaoController {
    @Autowired
    private DaService daService;
    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @RequestMapping(value = "/{aipId}", method = RequestMethod.GET)
    public List<DaDaoVO> getDaDaoByAipAndType(@PathVariable("aipId") final Integer aipId,
                                            @RequestParam("type") final DaDao.DaoType type) {
        List<DaDao> result = daService.findByAipIdAndTypeAndDeleteChangeIsNull(aipId, type);
        return result.stream().map(clientFactoryVO::createDaDaoVO).toList();
    }
}
