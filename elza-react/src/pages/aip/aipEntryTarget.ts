import {
    AbstractFilter,
    AipDetailVO,
    AipFieldName,
    FilterType,
    LogicalFilter,
    OperationLogicalType,
    OperationTextType,
    SearchParams,
} from 'elza-api';
import { aipCodeForms, sameAipCode } from 'components/aip/aipCode';
import { buildFilter } from 'components/aip/filter/aipFilterModel';
import { urlAip, urlFundAipExplorer, urlNode } from '../../constants';

const textEq = (fieldName: AipFieldName, value: string): AbstractFilter =>
    buildFilter(fieldName, 'text', { operation: OperationTextType.Eq, value });

/**
 * AIP search for the entry URL /da/<repository>/aip/<aipCode>: the code the digital archive gave
 * the AIP, within that archive, in any form it may be stored in (see aipCodeForms).
 */
export function aipEntrySearch(repositoryCode: string, aipCode: string): SearchParams {
    const anyCode: LogicalFilter = {
        filterType: FilterType.Logical,
        operation: OperationLogicalType.Or,
        filters: aipCodeForms(aipCode).map(code => textEq(AipFieldName.Code, code)),
    };
    return {
        filters: [textEq(AipFieldName.DigitalRepositoryCode, repositoryCode.trim()), anyCode],
        offset: 0,
        size: 2,
    };
}

/** The AIP found by aipEntrySearch; the code exactly as written wins over its other forms. */
export function aipEntryFound(rows: AipDetailVO[] | undefined, aipCode: string): AipDetailVO | undefined {
    return rows?.find(row => row.code === aipCode.trim()) ?? rows?.[0];
}

/**
 * Where the entry URL leads: straight to the unit of description when exactly one is linked,
 * otherwise to the package in its fund, where the linked units are listed and the package can
 * be connected. An AIP without a known fund opens in the AIP list.
 *
 * With a part, the units linked to that part count; when there is none, a unit holding the
 * whole package contains the part as well. Without a single unit the explorer selects the part.
 */
export function aipEntryTarget(aip: AipDetailVO, part?: string | null): string {
    const links = aip.linkedNodes ?? [];
    const nodeIds = (linked: typeof links) => new Set(linked.map(link => link.nodeId));
    let candidates = nodeIds(part ? links.filter(link => link.daoCode && sameAipCode(link.daoCode, part)) : links);
    if (part && candidates.size === 0) {
        candidates = nodeIds(links.filter(link => !link.daoCode));
    }
    if (candidates.size === 1) {
        return urlNode([...candidates][0]);
    }
    if (aip.fund?.id != null) {
        return part
            ? urlFundAipExplorer(aip.fund.id, aip.aipId, undefined, 'structure', part.trim())
            : urlFundAipExplorer(aip.fund.id, aip.aipId);
    }
    return urlAip(aip.aipId);
}
