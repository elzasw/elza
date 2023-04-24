package cz.tacr.elza.domain.bridge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Místo pro načtení konfigurace Lucene indexu, před vlastní inicializací indexu.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Component
public class IndexConfigurationReader {

    //Toto NEFUNGUJE!! závislost na bean co je závislý na Hibernate
    //@Autowired
    //AeBatchRepository aeBatchRepository;

    //Toto je OK funguje
    @Autowired
    JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        //jdbcTemplate.
    }

}
