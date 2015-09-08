package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Pravidla tvorby AP. Primárními pravidly jsou Základní pravidla. Je možné však připravit jiná
 * pravidla tvorby, případně stávající pravidla dále rozpracovat a modifikovat. Je realizována pouze
 * entita obalující základní pravidla, nikoli reálná základní pravdial
 * 
 * @author vavrejn
 *
 */
public interface RulRuleSet extends Serializable {

    Integer getRuleSetId();

    void setRuleSetId(Integer ruleSetId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);
}
