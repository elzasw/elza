package cz.tacr.elza.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.RegExternalSourceVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;


/**
 * Konfigurace továrny na VO objekty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */

@Configuration
public class ConfigMapperConfiguration {

    /**
     * @return Tovární třída.
     */
    @Bean(name = "configVOMapper")
    public MapperFactory configVOMapper() {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        initSimpleVO(mapperFactory);

        return mapperFactory;
    }

    /**
     * Vytvoří mapování jednoduchý objektů.
     *
     * @param mapperFactory tovární třída
     */
    private void initSimpleVO(final MapperFactory mapperFactory) {
        mapperFactory.classMap(ParPartyType.class, ParPartyTypeVO.class).byDefault().register();

        mapperFactory.classMap(RegRecord.class, RegRecordVO.class)
                .exclude("variantRecordList")
                .customize(new CustomMapper<RegRecord, RegRecordVO>() {
                    @Override
                    public void mapAtoB(final RegRecord regRecord,
                                        final RegRecordVO regRecordVO,
                                        final MappingContext context) {
                        RegRecord parentRecord = regRecord.getParentRecord();
                        if (parentRecord != null) {
                            regRecordVO.setParentRecordId(parentRecord.getRecordId());
                        }
                    }
                }).byDefault().register();

        mapperFactory.classMap(RegExternalSource.class, RegExternalSourceVO.class).byDefault().register();


        mapperFactory.classMap(RegRegisterType.class, RegRegisterTypeVO.class).customize(
                new CustomMapper<RegRegisterType, RegRegisterTypeVO>() {
                    @Override
                    public void mapAtoB(final RegRegisterType regRegisterType,
                                        final RegRegisterTypeVO regRegisterTypeVO,
                                        final MappingContext context) {
                        RegRegisterType parentType = regRegisterType.getParentRegisterType();
                        if (parentType != null) {
                            regRegisterTypeVO.setParentRegisterTypeId(parentType.getRegisterTypeId());
                        }
                    }
                }).byDefault()
                .register();

    }

}
