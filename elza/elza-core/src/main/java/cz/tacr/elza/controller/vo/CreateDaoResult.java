package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDao;

public record CreateDaoResult(ArrDao dao, List<String> skippedEntries) {

}
