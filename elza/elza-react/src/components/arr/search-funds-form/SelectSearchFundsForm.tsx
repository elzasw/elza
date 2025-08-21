import { useState, ReactNode } from 'react';
import { Modal, FormCheck } from 'react-bootstrap';
import classNames from 'classnames';
import { createReferenceMark, getNodeIcon } from 'components/arr/ArrUtils.jsx'
import { i18n, Icon } from 'components/shared';
import Search from "../../shared/search/Search";
import HorizontalLoader from "../../shared/loading/HorizontalLoader";
import './SearchFundsForm.scss';
import { FundSearchFundType, FundSearchNodeType } from 'typings/store/index.js';
import { FieldType, FilterType, NodeFieldName, OperationCompareType } from 'elza-api';
import { Api } from 'api';

const FUND_NAME_MAX_CHARS = 60;

interface SubmitDataType {
    node: FundSearchNodeType;
    fund: FundSearchFundType;
}

interface Props {
    onSubmit: (data: SubmitDataType) => void;
}

export const SelectSearchFundsForm = ({ onSubmit }: Props) => {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [query, setQuery] = useState<string>("");
    const [isIdSearch, setIsIdSearch] = useState<boolean>(false);
    const [funds, setFunds] = useState<FundSearchFundType[]>([]);
    const [isFetching, setIsFetching] = useState(false);
    const [isFetched, setIsFetched] = useState(false);

    /**
* Vyhledání v archivních souborech.
*/
    const handleSearch = async (fulltext: string) => {
        setQuery(fulltext);
        setIsFetching(true);

        const filter = isIdSearch ? {
            filterType: FilterType.FieldValue,
            field: {
                fieldType: FieldType.NodeField,
                fieldName: NodeFieldName.Uuid,
            },
            value: fulltext,
            operation: OperationCompareType.Contains,
        } : {
            filterType: FilterType.Contains,
            value: fulltext,
        }

        const { data } = await Api.node.nodeSearch({
            filters: [filter],
        })

        setFunds(data.fonds.map((fund) => ({
            ...fund,
            expanded: false,
            nodes: [],
            fetched: false,
            isFetching: false,
            icon: undefined,
        })))

        setIsFetching(false);
        setIsFetched(true);
    };

    const handleRadioChange = (_isIdSearch: boolean) => () => {
        setIsIdSearch(_isIdSearch);
    };
    /**
* Smazání výsledků vyhledávání.
*/
    const handleClearSearch = () => {
        setQuery("")
    };

    /**
* Zobrazení seznamu výskytů hledaného výrazu v AS
*/
    const handleFundExpand = async (fund: FundSearchFundType) => {
        if (!fund.expanded) {
            const { data } = await Api.node.nodeGetSearchResult(fund.id)
            const index = funds.findIndex(({ id }) => id === fund.id);
            const newFunds = [...funds];
            newFunds.splice(index, 1, { ...fund, nodes: data, expanded: true })
            setFunds(newFunds);
        } else {
            const index = funds.findIndex(({ id }) => id === fund.id);
            const newFunds = [...funds];
            newFunds.splice(index, 1, { ...fund, expanded: false })
            setFunds(newFunds);
        }
    };

    /**
* Přejít na detail uzlu
*/
    const handleNodeClick = (item: FundSearchNodeType) => {
        const itemFund = funds.find((fund) => fund.nodes.some((node) => node.id === item.id));
        if (!itemFund) { throw Error("Cannot submit node without fund") }

        setIsSubmitting(true);
        onSubmit({
            fund: itemFund,
            node: item
        });
    };

    /**
* Renderování vyhledaného archivního souboru.
*/
    const renderFund = (fund: FundSearchFundType) => {
        const { expanded } = fund;
        const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');

        const cls = classNames({
            item: true,
            opened: expanded,
            closed: !expanded,
        });
        fund.icon = '';

        let name = fund.name;
        if (name.length > FUND_NAME_MAX_CHARS) {
            name = name.substring(0, FUND_NAME_MAX_CHARS - 3) + '...'
        }

        return <div key={fund.id} className="fund">
            <div className={cls}>
                <span className={expColCls} onClick={() => handleFundExpand(fund)} />
                <Icon className="item-icon" glyph="fa-database" />
                <div title={fund.name} className="item-label">{name} {fund.count && `(${fund.count})`}</div>
            </div>
            {expanded && fund.nodes &&
                <div className="nodes">
                    {fund.nodes.map((node) => renderNode(node))}
                </div>
            }
        </div>;
    };

    /**
* Render JP.
*/
    const renderNode = (node: FundSearchNodeType) => {
        const levels = createReferenceMark(node, null, undefined);
        const iconProps = getNodeIcon(true, node.icon);
        return <div key={node.id} className="node">
            <div className="levels">{levels}</div>
            <Icon className="item-icon" {...iconProps} />
            <div title={node.name} className="item-label">{node.name}</div>
            <span className="detail-col fa fa-sign-out" onClick={() => handleNodeClick(node)} />
        </div>
    };

    const renderResult = () => {
        const result: ReactNode[] = [];

        if (funds.length > 0) {
            result.push(
                <div key="result" className="result-list">
                    {funds.length > 0 &&
                        funds.map(fund => renderFund(fund))
                    }
                </div>
            )
        }

        return result;
    };

    const getTotalCount = (funds: FundSearchFundType[]) => {
        let count = 0;
        funds.forEach(fund => count += fund.count);
        return count;
    }

    if (isSubmitting) {
        return <Modal.Body>
            <HorizontalLoader hover showText={false} key="loader" />
        </Modal.Body>
    }

    const totalCount = getTotalCount(funds);

    return (
        <Modal.Body>
            <div className="horizontal-radio">
                <FormCheck
                    label={i18n("arr.fund.search.fulltext")}
                    type="radio"
                    name="searchType"
                    onChange={handleRadioChange(false)}
                    checked={!isIdSearch}
                />
                <FormCheck
                    label={i18n("arr.fund.search.id")}
                    type="radio"
                    name="searchType"
                    onChange={handleRadioChange(true)}
                    checked={isIdSearch}
                />
            </div>
            <Search
                onSearch={handleSearch}
                onClear={handleClearSearch}
                placeholder={i18n('search.input.search')}
                value={query}
            />
            {isFetching && <HorizontalLoader hover showText={false} key="loader" />}
            {isFetched && i18n('arr.fund.search.result.count', totalCount)}
            <div className={`fund-search ${isFetched && totalCount > 0 ? 'result' : 'no-fulltext'}`}>
                {isFetched
                    ? renderResult()
                    : i18n('arr.fund.search.noFulltext'
                    )}
            </div>
        </Modal.Body >
    )

}
