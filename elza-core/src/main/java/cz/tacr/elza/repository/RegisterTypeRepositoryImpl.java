package cz.tacr.elza.repository;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.RegRegisterType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
public class RegisterTypeRepositoryImpl implements RegisterTypeRepositoryCustom {

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Override
    public Set<Integer> findSubtreeIds(final Integer registerTypeId) {
        Set<Integer> result = new HashSet<>();
        result.add(registerTypeId);

        for (RegRegisterType regRegisterType : registerTypeRepository.findAll()) {
            RegRegisterType parent = regRegisterType;
            while (parent != null) {
                if (parent.getRegisterTypeId().equals(registerTypeId)) {
                    result.add(regRegisterType.getRegisterTypeId());
                    break;
                }

                parent = parent.getParentRegisterType();
            }

        }

        return result;
    }
}
