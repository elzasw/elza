package cz.tacr.elza.service.output.dev;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
public class OutputGeneratorService {

    @Bean
    @Scope("prototype")
    public OutputGeneratorWorker createConformityInfoWorker(Integer fundVersionId) {
        return new OutputGeneratorWorker(fundVersionId);
    }
}
