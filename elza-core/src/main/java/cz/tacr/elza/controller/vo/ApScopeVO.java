package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysLanguage;

/**
 * Třída rejstříku.
 *
 * @since 27.01.2016
 */
public class ApScopeVO
        extends BaseCodeVo {

    private String language;

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
        entity.setCode(getCode());
        entity.setName(getName());
        entity.setScopeId(getId());
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
