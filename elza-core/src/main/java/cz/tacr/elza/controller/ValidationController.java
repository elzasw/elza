package cz.tacr.elza.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ValidationResult;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;


/**
 * Validátor obecných hodnot.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 06.01.2016
 */
@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    /**
     * Provede validaci data hodnoty atributu.
     *
     * @param value textová hodnota
     * @return objekt s validní nebo nevalidní zprávou
     */
    @RequestMapping(value = "/unitDate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationResult getFundVersions(@RequestParam(value = "value", required = true) final String value) {

        try {
            UnitDateConvertor.convertToUnitDate(value, new ArrDescItemUnitdate());
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage());
        }

        return new ValidationResult(true);
    }
}
