package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import cz.tacr.elza.repository.RuleSetRepository;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.service.RuleService;


/**
 * Kontroler pro pravidla.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
@RestController
@RequestMapping("/api/ruleSetManagerV2")
public class RuleController {

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private RuleService ruleService;
    @Autowired
    private PackageService packageService;

    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSetVO> getRuleSets() {
        return factoryVo.createRuleSetList(ruleSetRepository.findAll());
    }

    @RequestMapping(value = "/dataTypes", method = RequestMethod.GET)
    public List<RulDataTypeVO> getDataTypes() {
        List<RulDataType> dataTypes = dataTypeRepository.findAll();
        return factoryVo.createDataTypeList(dataTypes);
    }

    @RequestMapping(value = "/descItemTypes", method = RequestMethod.GET)
    public List<RulDescItemTypeExtVO> getDescItemTypes() {
        List<RulDescItemTypeExt> descItemTypes = ruleService.getAllDescriptionItemTypes();
        return factoryVo.createDescItemTypeExtList(descItemTypes);
    }

    @RequestMapping(value = "/getPackages", method = RequestMethod.GET)
    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }

    @RequestMapping(value = "/deletePackage/{code}", method = RequestMethod.GET)
    @Transactional
    public void deletePackage(@PathVariable(value = "code") final String code) {
        Assert.notNull(code);
        packageService.deletePackage(code);
    }

    @RequestMapping(value = "/exportPackage/{code}", method = RequestMethod.GET)
    public void exportPackageRest(@PathVariable(value = "code") final String code,
                                  HttpServletResponse response) {
        Assert.notNull(code);
        try {
            File file = packageService.exportPackage(code);
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "inline; filename=" + code + "-package.zip");
            response.setContentLength((int) file.length());
            InputStream is = new FileInputStream(file);
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("Problem pri zapisu souboru", ex);
        }
    }


    @Transactional
    @RequestMapping(value="/importPackage", method=RequestMethod.POST)
    public void importPackageRest(@RequestParam("file") final MultipartFile file){
        Assert.notNull(file);
        File temp = null;
        try {
            temp = File.createTempFile("importPackage", ".zip");
            file.transferTo(temp);
            packageService.importPackage(temp);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit dočasný soubor pro import", e);
        } finally {
            if (temp != null) {
                temp.deleteOnExit();
            }
        }
    }

}
