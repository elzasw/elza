import { Modal } from 'react-bootstrap';
import classNames from 'classnames';
import { createReferenceMark, getNodeIcon } from 'components/arr/ArrUtils.jsx';
import { i18n, Icon, HorizontalLoader } from 'components/shared';

import { routerNavigate } from 'actions/router.jsx';
import { modalDialogHide } from 'actions/global/modalDialog.jsx';
import * as fundSearchActions from 'actions/arr/fundSearch.jsx';

import './SearchFundsForm.scss';
import { urlFundNode } from "../../../constants.js";
// import { InputOnChangeData, SearchBox, SearchBoxChangeEvent } from '@fluentui/react-components';
// import { Form, Field } from 'react-final-form';
import { AppState, FundSearchFundType, FundSearchNodeType } from 'typings/store';
import { useSelector } from 'react-redux';
import { useThunkDispatch } from 'utils/hooks';
import { useEffect } from 'react';
import { NodeSearchFilters } from './filters/NodeSearchFilters';
import { FilterObject } from './filters/types';

const FUND_NAME_MAX_CHARS = 60;

export function SearchFundsFormFn() {
    // const [currentFilters, setCurrentFilters] = useState<FilterObject[]>([]);
    const arrRegion = useSelector((state: AppState) => state.arrRegion);
    const { fundSearch } = arrRegion;
    const dispatch = useThunkDispatch();

    useEffect(() => {
        dispatch(fundSearchActions.fundSearchFetchIfNeeded());
    }, [dispatch, fundSearch])

    useEffect(() => {
        if (fundSearch.isIdSearch) {
            dispatch(fundSearchActions.fundSearchFulltextChange({ isIdSearch: false, fulltext: undefined }));
            dispatch(fundSearchActions.fundSearchFulltextClear());

        }
        else {
            dispatch(fundSearchActions.fundSearchFetchIfNeeded());
        }
    }, [dispatch, fundSearch.isIdSearch])

    const handleFluentSearch = (filters: FilterObject[]) => {
        console.log('#fs', filters);
        // setCurrentFilters(filters);
        dispatch(fundSearchActions.fundSearchFiltersChange({
            filters: filters
        }));
    };

    const handleRefresh = () => {
        dispatch(fundSearchActions.fundSearchFetchIfNeeded(true));
    }

    /**
     * Zobrazení seznamu výskytů hledaného výrazu v AS
     */
    const handleFundClick = (fund: FundSearchFundType) => {
        dispatch(fundSearchActions.fundSearchExpandFund(fund));
    };

    /**
     * Přejít na detail uzlu
     */
    const handleNodeClick = (nodeId: number, fundId: number) => {
        // Přepnutí na jednotku popisu v příslušném archivním souboru
        dispatch(routerNavigate(urlFundNode(fundId, undefined, nodeId)));
        dispatch(modalDialogHide());
    };

    /**
     * Renderování vyhledaného archivního souboru.
     */
    function renderFund(fund: FundSearchFundType) {
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
            name = name.substring(0, FUND_NAME_MAX_CHARS - 3) + '...';
        }

        return (
            <div key={fund.id} className="fund">
                <div className={cls}>
                    <span className={expColCls} onClick={() => handleFundClick(fund)} />
                    <Icon className="item-icon" glyph="fa-database" />
                    <div title={fund.name} className="item-label">
                        {name} {fund.count && `(${fund.count})`}
                    </div>
                </div>
                {expanded && fund.nodes &&
                    <div className="nodes">{fund.nodes.map((node) => renderNode(node, fund))}</div>}
            </div>
        );
    }

    function renderResult() {
        const result = [];

        if (fundSearch.fetched) {
            result.push(
                <div key="result" className="result-list">
                    {fundSearch.funds.length > 0 && fundSearch.funds.map((fund) => renderFund(fund))}
                </div>
            );
        }

        return result;
    }

    function getTotalCount(funds: FundSearchFundType[]) {
        let count = 0;
        funds.forEach(fund => (count += fund.count));
        return count;
    }

    /**
     * Render JP.
     */
    function renderNode(node: FundSearchNodeType, fund: FundSearchFundType) {
        const levels = createReferenceMark(node, null, undefined);
        const iconProps = getNodeIcon(true, node.icon);
        return (
            <div key={node.id} className="node" onClick={() => handleNodeClick(node.id, fund.id)}>
                <div className="levels">{levels}</div>
                <Icon className="item-icon" {...iconProps} />
                <div title={node.name} className="item-label">
                    {node.name}
                </div>
            </div>
        );
    }

    const isFulltext = fundSearch.filters.length > 0;
    const displayedCount = getTotalCount(fundSearch.funds);

    return (
        <Modal.Body className="search-funds-form">
            {/* <Form initialValues={{ fulltext: fundSearch.fulltext }} onSubmit={handleFluentSearch}> */}
            {/*     {({ handleSubmit, form }) => { */}
            {/*         return <form onSubmit={handleSubmit}> */}
            {/*             <Field name="fulltext"> */}
            {/*                 {({ input }) => { */}
            {/*                     const handleChange = (e: SearchBoxChangeEvent, data: InputOnChangeData) => { */}
            {/*                         input.onChange(e); */}
            {/*                         // Submit form on value reset */}
            {/*                         if (e.type === "click" && data.value === "") { */}
            {/*                             form.submit(); */}
            {/*                         } */}
            {/*                     } */}
            {/*                     return <SearchBox {...input} type='search' onChange={handleChange} /> */}
            {/*                 }} */}
            {/*             </Field> */}
            {/*         </form> */}
            {/*     }} */}
            {/* </Form> */}
            <NodeSearchFilters onChange={(filters) => handleFluentSearch(filters)} onRefresh={handleRefresh} currentFilters={fundSearch.filters} />
            {fundSearch.isFetching && <HorizontalLoader hover showText={false} key="loader" />}
            {isFulltext && <>
                {i18n('arr.fund.search.result.count', fundSearch.totalCount)}
                {fundSearch.partialResult && <>&nbsp;({i18n('arr.fund.search.result.displayedCount', displayedCount)})</>}
            </>}
            <div className={`fund-search ${isFulltext && displayedCount > 0 ? 'result' : 'no-fulltext'}`}>
                {isFulltext ? renderResult() : i18n('arr.fund.search.noFulltext')}
            </div>
        </Modal.Body>
    );
}

export default SearchFundsFormFn;
