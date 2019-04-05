package cz.tacr.elza.config;


import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.config.view.FundViewConfigs;
import cz.tacr.elza.config.view.ViewTitles;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.packageimport.xml.SettingFundViews.FundView;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;


/**
 * Nastavení popisků v UI.
 *
 * @since 10.12.2015
 */
@Component
@EventBusListener
public class ConfigView {

    private static final Logger logger = LoggerFactory.getLogger(ConfigView.class);

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Nastavení zobrazení v UI.
     * 
     * Mapa mezi pravidly a zobrazením
     */
    private FundViewConfigs fundViews;

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.VIEW)) {
            if (fundViews != null) {
                logger.info("Fund views invalidated.");
            }
            fundViews = null;
        }
    }

    public ViewTitles getViewTitles(final Integer ruleSetId, final Integer fundId) {
        Validate.notNull(ruleSetId, "Kód musí být vyplněn");
        Validate.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        FundViewConfigs fvs = getFundViews();
        Validate.notNull(fvs);

        //TODO: Try to find by fund id
        //ViewTitles viewByFa = fvs.getByFundId(FA_PREFIX + fundId);

        ViewTitles vt = fvs.getByRuleSetId(ruleSetId);
        if (vt == null) {
            throw new SystemException("Missing view configuration", BaseCode.INVALID_STATE)
                    .set("ruleSetId", ruleSetId);
        }
        return vt;
    }

    public FundViewConfigs getFundViews() {
        if (fundViews == null) {
            fundViews = loadFundViews();
        }
        return fundViews;
    }

    private FundViewConfigs loadFundViews() {
        StaticDataProvider sdp = staticDataService.getData();
        
        FundViewConfigs result = new FundViewConfigs();
        
        // load relevant settings
        List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, UISettings.SettingsType.FUND_VIEW.toString(), UISettings.EntityType.RULE);

        uiSettingsList.forEach(uiSettings -> {
            SettingFundViews setting = SettingFundViews.newInstance(uiSettings);
            FundView xmlFundView = setting.getFundView();

            ViewTitles vt = ViewTitles.valueOf(xmlFundView, sdp);

            RulRuleSet rulRuleSet = sdp.getRuleSetById(uiSettings.getEntityId());

            result.addRuleSetFundView(rulRuleSet.getRuleSetId(), vt);
        });
        return result;
    }
}
