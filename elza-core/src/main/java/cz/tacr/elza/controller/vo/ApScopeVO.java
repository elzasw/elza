package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysLanguage;

/**
 * Třída rejstříku.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
public class ApScopeVO {

    private Integer id;

    private String code;

    private String name;

    private String language;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }
    
    /**
     * Creates AP scope from this value object.
     */
    public ApScope createEntity(StaticDataProvider staticData) {
        ApScope entity = new ApScope();
        entity.setCode(code);
        entity.setName(name);
        entity.setScopeId(id);
        if (StringUtils.isNotEmpty(language)) {
            SysLanguage lang = staticData.getSysLanguageByCode(language);
            entity.setLanguage(Validate.notNull(lang));
        }
        return entity;
    }
    
    /**
     * Creates value object from AP scope.
     */
    public static ApScopeVO newInstance(ApScope src, StaticDataProvider staticData) {
        ApScopeVO vo = new ApScopeVO();
        vo.setCode(src.getCode());
        vo.setId(src.getScopeId());
        vo.setName(src.getName());
        if (src.getLanguageId() != null) {
            SysLanguage lang = staticData.getSysLanguageById(src.getLanguageId());
            vo.setLanguage(lang.getCode());
        }
        return vo;
    }
}
