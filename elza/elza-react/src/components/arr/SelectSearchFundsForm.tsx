import React, { useState, ReactNode, useEffect } from 'react';
import { useSelector } from "react-redux";
import { Modal, FormCheck } from 'react-bootstrap';
import classNames from 'classnames';
import { createReferenceMark, getNodeIcon } from 'components/arr/ArrUtils.jsx'
import { i18n, Icon } from 'components/shared';
import { fundSearchFulltextClear, fundSearchFulltextChange, fundSearchExpandFund, fundSearchFetchIfNeeded } from '../../actions/arr/fundSearch'
import Search from "../shared/search/Search";
import HorizontalLoader from "../shared/loading/HorizontalLoader";
import './SearchFundsForm.scss';
import { AppState, FundSearchFundType, FundSearchNodeType } from 'typings/store/index.js';
import { useThunkDispatch } from 'utils/hooks';

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
    const dispatch = useThunkDispatch();
    const arrRegion = useSelector(({ arrRegion }: AppState) => (arrRegion));
    const { fundSearch } = arrRegion;

    /**
* Vyhledání v archivních souborech.
*/
    const handleSearch = (fulltext: string) => {
        dispatch(fundSearchFulltextChange({ fulltext, isIdSearch: undefined }));
    };

    const handleRadioChange = (isIdSearch: boolean) => () => {
        dispatch(fundSearchFulltextClear());
        dispatch(fundSearchFulltextChange({ fulltext: undefined, isIdSearch }));
    };
    /**
* Smazání výsledků vyhledávání.
*/
    const handleClearSearch = () => {
        dispatch(fundSearchFulltextClear());
    };

    /**
* Zobrazení seznamu výskytů hledaného výrazu v AS
*/
    const handleFundClick = (fund: FundSearchFundType) => {
        dispatch(fundSearchExpandFund(fund));
    };

    /**
* Přejít na detail uzlu
*/
    const handleNodeClick = (item: FundSearchNodeType) => {
        const itemFund = fundSearch.funds.find((fund) => fund.nodes.some((node) => node.id === item.id));
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

        let cls = classNames({
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
                <span className={expColCls} onClick={() => handleFundClick(fund)} />
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

        if (fundSearch.fetched) {
            result.push(
                <div key="result" className="result-list">
                    {fundSearch.funds.length > 0 &&
                        fundSearch.funds.map(fund => renderFund(fund))
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

    useEffect(() => {
        dispatch(fundSearchFetchIfNeeded());
    }, [fundSearch])

    if (isSubmitting) {
        return <Modal.Body>
            <HorizontalLoader hover showText={false} key="loader" />
        </Modal.Body>
    }

    const isFulltext = fundSearch.fulltext.length > 0;
    const totalCount = getTotalCount(fundSearch.funds);

    return (
        <Modal.Body>
            <div className="horizontal-radio">
                <FormCheck
                    label={i18n("arr.fund.search.fulltext")}
                    type="radio"
                    name="searchType"
                    onChange={handleRadioChange(false)}
                    checked={!fundSearch.isIdSearch}
                />
                <FormCheck
                    label={i18n("arr.fund.search.id")}
                    type="radio"
                    name="searchType"
                    onChange={handleRadioChange(true)}
                    checked={fundSearch.isIdSearch}
                />
            </div>
            <Search
                onSearch={handleSearch}
                onClear={handleClearSearch}
                placeholder={i18n('search.input.search')}
                value={fundSearch.fulltext}
            />
            {fundSearch.isFetching && <HorizontalLoader hover showText={false} key="loader" />}
            {isFulltext && i18n('arr.fund.search.result.count', totalCount)}
            <div className={`fund-search ${isFulltext && totalCount > 0 ? 'result' : 'no-fulltext'}`}>
                {isFulltext
                    ? renderResult()
                    : i18n('arr.fund.search.noFulltext'
                    )}
            </div>
        </Modal.Body >
    )

}
