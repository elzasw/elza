/**
 * Hromadné úpravy AS, tabulkové zobrazení.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    Icon,
    AbstractReactComponent,
    i18n,
    Loading,
    StoreHorizontalLoader,
    HorizontalLoader,
    DataGrid,
    DataGridColumnsSettings,
    DataGridPagination,
    SearchWithGoto
} from 'components/shared';
import FundBulkModificationsForm from './FundBulkModificationsForm';
import FundFilterSettings from './FundFilterSettings';
import FundDataGridCellForm from './FundDataGridCellForm';
import ArrSearchForm from './ArrSearchForm';
import {Button, MenuItem} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {
    fundDataGridSetColumnsSettings,
    fundDataGridSetSelection,
    fundDataGridSetColumnSize,
    fundDataGridFetchFilterIfNeeded,
    fundDataGridFetchDataIfNeeded,
    fundDataGridSetPageIndex,
    fundDataGridSetPageSize,
    fundDataGridFilterChange,
    fundBulkModifications,
    fundDataGridFilterClearAll,
    fundDataGridPrepareEdit,
    fundDataFulltextSearch,
    fundDataFulltextPrevItem,
    fundDataFulltextNextItem,
    fundDataChangeCellFocus,
    fundDataChangeRowIndexes,
    fundDataFulltextClear,
    fundDataFulltextExtended,
    fundDataInitIfNeeded,
    fundDataGridRefreshRows
} from 'actions/arr/fundDataGrid.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundSelectSubNode} from 'actions/arr/node.jsx';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {
    createReferenceMarkFromArray,
    getSpecsIds,
    hasDescItemTypeValue,
    createFundRoot
} from 'components/arr/ArrUtils.jsx'
import {getMapFromList, getSetFromIdsList} from 'stores/app/utils.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {COL_DEFAULT_WIDTH, COL_REFERENCE_MARK} from "./FundDataGridConst";
import './FundDataGrid.less'
import {getPagesCount} from "../shared/datagrid/DataGridPagination";
import {FILTER_NULL_VALUE} from 'actions/arr/fundDataGrid.jsx'
import {toDuration} from "../validate";
import {DisplayType} from "../../constants.tsx";
import Moment from 'moment';
import * as groups from "../../actions/refTables/groups"

class FundDataGrid extends AbstractReactComponent {
    static PropTypes = {
        fundId: React.PropTypes.number.isRequired,
        versionId: React.PropTypes.number.isRequired,
        fund: React.PropTypes.object.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        ruleSet: React.PropTypes.object.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        closed: React.PropTypes.bool.isRequired,
        fundDataGrid: React.PropTypes.object.isRequired,    // store
    };

    constructor(props) {
        super(props);

        this.bindMethods('handleSelectedIdsChange', 'handleColumnResize', 'handleColumnSettings', 'handleChangeColumnsSettings',
            'handleBulkModifications', 'handleFilterSettings', 'headerColRenderer', 'cellRenderer', 'resizeGrid', 'handleFilterClearAll',
            'handleFilterUpdateData', 'handleContextMenu', 'handleSelectInNewTab', 'handleSelectInTab', 'handleEdit', 'handleEditClose',
            'handleFulltextSearch', 'handleFulltextChange', 'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleChangeFocus',
            'handleToggleExtendedSearch', 'handleChangeRowIndexes', 'fetchData');

        var colState = this.getColsStateFromProps(props, {fundDataGrid: {}})
        if (!colState) {
            colState = {cols: []}
        }

        colState.calendarTypesMap = getMapFromList(props.calendarTypes.items)

        this.state = colState
    }

    componentDidMount() {
        this.fetchData(this.props)

        this.setState({}, this.resizeGrid)

        // Pokud je potřeba aktualizovat, aktualizujeme při přepnutí na grid, ale zachováme stránkování
        const {versionId, fundDataGrid} = this.props
        if (fundDataGrid.rowsDirty || fundDataGrid.filterDirty) {
            this.dispatch(fundDataGridRefreshRows(versionId));
        }
    }

    fetchData(props) {
        const {fundDataGrid, descItemTypes, fund, versionId, ruleSet} = props;
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(groups.fetchIfNeeded(this.props.versionId));
        this.dispatch(refRulDataTypesFetchIfNeeded());
        this.dispatch(fundDataGridFetchFilterIfNeeded(versionId));
        this.dispatch(fundDataGridFetchDataIfNeeded(versionId, fundDataGrid.pageIndex, fundDataGrid.pageSize));
        // this.dispatch(refRuleSetFetchIfNeeded());
        if (ruleSet.fetched && descItemTypes.fetched && fund.activeVersion) {
            // Prvotní inicializace zobrazení, pokud ještě grid nebyl zobrazem
            // Určí se seznam viditelných sloupečků a případně i šířka
            if (Object.keys(fundDataGrid.visibleColumns).length === 0 && Object.keys(fundDataGrid.columnInfos).length === 0) {
                const initData = {visibleColumns: [], columnInfos: []}
                const ruleMap = getMapFromList(ruleSet.items);
                const rule = ruleMap[fund.activeVersion.ruleSetId];
                if (rule.gridViews) {
                    // mapa kodu prvku popisu
                    const codeMap = {};
                    descItemTypes.items.forEach(item => {
                        codeMap[item.code]=item.id;
                    });
                    // nastaveni vychozich hodnot
                    rule.gridViews.forEach(gw => {
                        if (gw.showDefault) {
                            const itemId = codeMap[gw.code];
                            if(itemId!=null) {
                                initData.visibleColumns.push(itemId);
                                if (gw.width) {
                                    initData.columnInfos[itemId] = {
                                        width: gw.width
                                    };
                                }
                            }
                        }
                    });
                }
                this.dispatch(fundDataInitIfNeeded(versionId, initData));
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        this.fetchData(nextProps)

        var colState = this.getColsStateFromProps(nextProps, this.props)
        if (!colState) {
            colState = {}
        }

        colState.calendarTypesMap = getMapFromList(nextProps.calendarTypes.items)

        this.setState(colState, this.resizeGrid)
    }

    resizeGrid() {
        const parentEl = ReactDOM.findDOMNode(this.refs.gridContainer)
        const gridEl = ReactDOM.findDOMNode(this.refs.grid)
        if (parentEl && gridEl) {
            const rect = parentEl.getBoundingClientRect()
            const width = rect.right - rect.left
            gridEl.style.width = width + 'px'
        }
    }

    referenceMarkCellRenderer(row, rowIndex, col, colIndex, cellFocus) {
        const referenceMark = row.referenceMark;

        let itemValue;
        if (referenceMark && referenceMark.length > 0) {
            itemValue = createReferenceMarkFromArray(referenceMark);
        } else {
            itemValue = "";
        }

        return <div className='cell-value-wrapper'>{itemValue}</div>
    }

    cellRenderer(row, rowIndex, col, colIndex, cellFocus) {
        const colValue = row[col.dataName]

        var displayValue
        if (colValue && colValue.values) {
            displayValue = colValue.values.map(value => {
                let itemValue
                if (hasDescItemTypeValue(col.dataType)) {
                    switch (col.dataType.code) {
                        case 'COORDINATES':
                            switch (value.geomType) {
                                case "B":
                                    itemValue = value.geomType + ": " + value.value
                                    itemValue = <span><Button disabled>{value.geomType}</Button> {value.value}</span>
                                    break
                                default:
                                    itemValue = <span><Button
                                        disabled>{value.geomType}</Button> {i18n('subNodeForm.countOfCoordinates', value.value)}</span>
                                    break
                            }
                            break;
                        case 'UNITDATE':
                            itemValue = this.state.calendarTypesMap[value.calendarTypeId].name.charAt(0) + ": " + value.value
                            break;
                        case 'JSON_TABLE':
                            itemValue = i18n("arr.fund.jsonTable.cell.title", col.refType.viewDefinition.length, value.rows);
                            break;
                        case 'INT':
                            const refType = col.refType;
                            if (refType.viewDefinition === DisplayType.DURATION) {
                                itemValue = toDuration(value.value);
                            } else {
                                itemValue = value.value;
                            }
                            break;
                        case 'DATE':
                            itemValue = Moment(value.value).format('l');
                            break;
                        default:
                            itemValue = value.value;
                            break
                    }
                }

                if (col.refType.useSpecification) {
                    var spec = null
                    for (var a = 0; a < col.refType.descItemSpecs.length; a++) {
                        if (col.refType.descItemSpecs[a].code === value.specCode) {
                            spec = col.refType.descItemSpecs[a]
                            break
                        }
                    }

                    return <div
                        className='cell-value-wrapper'>{hasDescItemTypeValue(col.dataType) ? spec.name + ': ' + itemValue : spec.name}</div>
                } else {
                    return <div className='cell-value-wrapper'>{itemValue}</div>
                }
            })
        }

        return (
            <div className=''>{displayValue}</div>
        )
    }

    supportBulkModifications(refType, dataType) {
        const {closed} = this.props

        if (refType.id === COL_REFERENCE_MARK) {
            return false;
        }

        return !closed;
        /*
         let result

         switch (dataType.code) {
         case 'TEXT':
         case 'STRING':
         case 'FORMATTED_TEXT':
         case 'UNITID':
         result = true
         break
         default:
         result = false
         break
         }
         return result*/
    }

    headerColRenderer(col) {
        const {fundDataGrid, readMode} = this.props

        var cls = 'cell'

        let filtered
        if (fundDataGrid.filter[col.refType.id]) {
            cls += ' filtered'
        }

        const showBulkModifications = this.supportBulkModifications(col.refType, col.dataType)

        return (
            <div className={cls} title={col.refType.name}>
                <div className="title">{col.refType.shortcut}</div>
                {showBulkModifications && !readMode &&
                <Button onClick={this.handleBulkModifications.bind(this, col.refType, col.dataType)}
                        title={i18n('arr.fund.bulkModifications.action')}><Icon glyph='fa-pencil'/></Button>}
                <Button onClick={this.handleFilterSettings.bind(this, col.refType, col.dataType)}
                        title={i18n('arr.fund.filterSettings.action')}><Icon glyph='fa-filter'/></Button>
            </div>
        )
    }

    getColsStateFromProps(nextProps, props) {
        const {fundDataGrid, descItemTypes, rulDataTypes} = nextProps;

        if (descItemTypes.fetched) {
            if (props.fundDataGrid.columnsOrder !== fundDataGrid.columnsOrder
                || props.descItemTypes !== descItemTypes
                || props.rulDataTypes !== rulDataTypes
                || props.fundDataGrid.columnInfos !== fundDataGrid.columnInfos
                || props.fundDataGrid.filter !== fundDataGrid.filter
                || props.fundDataGrid.visibleColumns !== fundDataGrid.visibleColumns
            ) {
                const cols = this.buildColumns(nextProps.fund, nextProps.ruleSet, fundDataGrid, descItemTypes, rulDataTypes)
                return {cols: cols}
            }
        }
        return null
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'descItemTypes', 'ruleSet', 'rulDataTypes', 'closed', 'readMode']
        if (!propsEquals(this.props, nextProps, eqProps)) {
            return true
        }

        var eqProps2 = [
            'isFetchingFilter', 'fetchedFilter', 'isFetchingData', 'fetchedData', 'pageSize', 'pageIndex', 'closed', 'readMode',
            'items', 'itemsCount', 'filter', 'visibleColumns', 'initialised', 'columnsOrder', 'columnInfos', 'selectedIds'
        ]
        if (!propsEquals(this.props.fundDataGrid, nextProps.fundDataGrid, eqProps2)) {
            return true
        }

        return false
    }

    getColumnsOrder(fundDataGrid, refTypesMap) {
        // Pořadí sloupečků - musíme brát i variantu, kdy není definované nebo kdy v něm některé atributy chybí
        var columnsOrder = []
        var map = {...refTypesMap}
        fundDataGrid.columnsOrder.forEach(id => {
            delete map[id]
            columnsOrder.push(id)
        })
        columnsOrder = [...columnsOrder, ...Object.keys(map)]
        return columnsOrder
    }

    buildColumns(fund, ruleSet, fundDataGrid, descItemTypes, rulDataTypes) {
        const refTypesMap = getMapFromList(descItemTypes.items)
        const dataTypesMap = getMapFromList(rulDataTypes.items)
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap)

        var cols = []

        // Sloupec s číslem JP
        const refMarkColInfo = fundDataGrid.columnInfos[COL_REFERENCE_MARK];
        cols.push({
            id: COL_REFERENCE_MARK,
            refType: {
                id: COL_REFERENCE_MARK,
                code: COL_REFERENCE_MARK,
                shortcut: i18n("arr.fund.title.referendeMark"),
            },
            title: i18n("arr.fund.title.referendeMark"),
            width: refMarkColInfo ? refMarkColInfo.width : COL_DEFAULT_WIDTH,
            headerColRenderer: this.headerColRenderer,
            cellRenderer: this.referenceMarkCellRenderer
        });

        // Vybrané sloupce
        const ruleMap = getMapFromList(ruleSet.items);
        const rule = ruleMap[fund.activeVersion.ruleSetId];
        const gwMap = {};   // mapa kódu item type na GridView
        if (rule.gridViews) {
            rule.gridViews.forEach(gw => {
                gwMap[gw.code] = gw;
            });
        }
        columnsOrder.forEach(id => {
            const refType = refTypesMap[id]
            if (fundDataGrid.visibleColumns[id]) {  // je vidět
                const colInfo = fundDataGrid.columnInfos[id];

                let width;
                if (colInfo) {  // je již definovaná
                    width = colInfo.width;
                } else
                    if (gwMap[refType.code] && gwMap[refType.code].width) {  // můžeme použít z pravidel
                    width = gwMap[refType.code].width;
                } else {    // dáme implicitní konstantu, nikde ji nemáme
                    width = COL_DEFAULT_WIDTH;
                }

                const col = {
                    id: refType.id,
                    refType: refType,
                    dataType: dataTypesMap[refType.dataTypeId],
                    title: refType.shortcut,
                    desc: refType.name,
                    width: width,
                    dataName: refType.id,
                    headerColRenderer: this.headerColRenderer,
                    cellRenderer: this.cellRenderer,
                };
                cols.push(col)
            }
        });

        return cols
    }

    handleColumnResize(colIndex, width) {
        const {versionId} = this.props
        this.dispatch(fundDataGridSetColumnSize(versionId, this.state.cols[colIndex].id, width))
    }

    handleSelectedIdsChange(ids) {
        const {versionId} = this.props
        this.dispatch(fundDataGridSetSelection(versionId, ids))
    }

    handleFilterClearAll() {
        const {versionId} = this.props
        this.dispatch(fundDataGridFilterClearAll(versionId))
    }

    handleFilterUpdateData() {
        const {versionId} = this.props
        this.dispatch(fundDataGridRefreshRows(versionId))
    }

    handleColumnSettings() {
        const {fundDataGrid, descItemTypes, fund, ruleSet} = this.props;

        var ruleMap = getMapFromList(ruleSet.items);
        var rule = ruleMap[fund.activeVersion.ruleSetId];

        let itemTypes = [];
        descItemTypes.items.forEach(item => {
            itemTypes.push(item);
        });

        const refTypesMap = getMapFromList(itemTypes);
        const columnsOrder = this.getColumnsOrder(fundDataGrid, refTypesMap);
        const visibleColumns = fundDataGrid.visibleColumns;

        const columns = columnsOrder.map(id => {
            const refType = refTypesMap[id];
            return {
                id: refType.id,
                name: refType.name,
                desc: refType.description
            }
        });

        this.dispatch(modalDialogShow(this, i18n('arr.fund.columnSettings.title'),
            <DataGridColumnsSettings
                onSubmitForm={this.handleChangeColumnsSettings}
                columns={columns}
                visibleColumns={visibleColumns}
            />
        ));
    }

    handleFilterSettings(refType, dataType) {
        const {versionId, calendarTypes, fundDataGrid} = this.props

        const otherFilters = {...fundDataGrid.filter};
        delete otherFilters[refType.id];

        this.dispatch(modalDialogShow(this, i18n('arr.fund.filterSettings.title', refType.shortcut),
            <FundFilterSettings
                versionId={versionId}
                refType={refType}
                dataType={dataType}
                calendarTypes={calendarTypes}
                filter={fundDataGrid.filter[refType.id]}
                filters={otherFilters}
                onSubmitForm={this.handleChangeFilter.bind(this, versionId, refType)}
            />, 'fund-filter-settings-dialog'
        ));
    }

    handleChangeFilter(versionId, refType, filter) {
        this.dispatch(modalDialogHide())
        this.dispatch(fundDataGridFilterChange(versionId, refType.id, filter))
    }

    handleBulkModifications(refType, dataType) {
        const {versionId, fundDataGrid, calendarTypes} = this.props;

        const submit = (data) => {
            // Sestavení seznamu node s id a verzí, pro které se má daná operace provést
            let nodes;
            let selectionType;
            switch (data.itemsArea) {
                case 'page':
                    nodes = fundDataGrid.items.map(i => ({id: i.node.id, version: i.node.version}));
                    selectionType = 'NODES';
                    break;
                case 'selected': {
                    const set = getSetFromIdsList(fundDataGrid.selectedIds);
                    nodes = [];
                    selectionType = 'NODES';
                    fundDataGrid.items.forEach(i => {
                        if (set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    });
                    break
                }
                case 'unselected': {
                    nodes = [];
                    selectionType = 'NODES';
                    const set = getSetFromIdsList(fundDataGrid.selectedIds);
                    fundDataGrid.items.forEach(i => {
                        if (!set[i.id]) {
                            nodes.push({id: i.node.id, version: i.node.version})
                        }
                    });
                    break
                }
                case 'all':
                    nodes = [];
                    selectionType = 'FUND';
                    break
            }

            // Zpracování hodnoty pro odeslání - musíme ji správně převést do testového formáltu pro odeslání na serveru
            let replaceText = data.replaceText;
            if (replaceText) {
                switch (dataType.code) {
                    case "UNITDATE":
                        if (typeof replaceText === 'object') {
                            replaceText = replaceText.calendarTypeId + '|' + replaceText.value;
                        }
                        break;
                    case "RECORD_REF":
                        replaceText = replaceText.id;
                        break;
                    default:
                        // standardně nic neděláme, zpracovávají se jen speciální typy
                        break;
                }
            }

            // Získání seznam specifikací
            const refTypeX = {...refType, descItemSpecs: [{id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')}, ...refType.descItemSpecs]};
            let specsIds = getSpecsIds(refTypeX, data.specs.type, data.specs.ids);
            specsIds = specsIds.map(specsId => specsId !== FILTER_NULL_VALUE ? specsId : null);
            if (selectionType !== 'FUND' || confirm(i18n('arr.fund.bulkModifications.warn'))) {
                return this.dispatch(fundBulkModifications(versionId, refType.id, specsIds, data.operationType, data.findText, replaceText, data.replaceSpec, nodes, selectionType))
            }
        };

        this.dispatch(modalDialogShow(this, i18n('arr.fund.bulkModifications.title'),
            <FundBulkModificationsForm
                refType={refType}
                dataType={dataType}
                calendarTypes={calendarTypes}
                onSubmitForm={submit}
                allItemsCount={fundDataGrid.items.length}
                checkedItemsCount={fundDataGrid.selectedIds.length}
            />
        ));
    }

    handleChangeColumnsSettings(columns) {
        const {versionId} = this.props;

        var visibleColumns = {};
        var columnsOrder = [];
        columns.forEach(col => {
            visibleColumns[col.id] = true;
            columnsOrder.push(col.id)
        });
        this.dispatch(fundDataGridSetColumnsSettings(versionId, visibleColumns, columnsOrder));

        this.dispatch(modalDialogHide())
    }

    handleContextMenu(row, rowIndex, col, colIndex, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem
                    onClick={this.handleSelectInNewTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.openInNewTab')}</MenuItem>
                <MenuItem
                    onClick={this.handleSelectInTab.bind(this, row)}>{i18n('arr.fund.bulkModifications.action.open')}</MenuItem>
                <MenuItem onClick={() => {
                    this.dispatch(contextMenuHide());
                    this.handleEdit(row, rowIndex, col, colIndex)
                }}>{i18n('global.action.update')}</MenuItem>
            </ul>
        );

        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y: e.clientY}));
    }

    handleEdit(row, rowIndex, col, colIndex) {
        const {versionId, fundId, closed} = this.props;
        const parentNodeId = row.parentNode ? row.parentNode.id : null;

        if (typeof col.id === 'undefined') {
            return
        }

        if (col.id === COL_REFERENCE_MARK) {
            return;
        }

        this.dispatch(fundDataGridPrepareEdit(versionId, row.id, parentNodeId, col.id));

        const dataGridComp = this.refs.dataGrid.getWrappedInstance();
        const cellEl = dataGridComp.getCellElement(rowIndex, colIndex);
        const cellRect = cellEl.getBoundingClientRect();

        let x = cellRect.left;
        let y = cellRect.top;
        const dataGridCompRect = ReactDOM.findDOMNode(dataGridComp).getBoundingClientRect();
        if (x < dataGridCompRect.left) {
            x = dataGridCompRect.left;
        }
        if (y < dataGridCompRect.top) {
            y = dataGridCompRect.top;
        }

        this.dispatch(modalDialogShow(this, null,
            <FundDataGridCellForm
                versionId={versionId}
                fundId={fundId}
                routingKey='DATA_GRID'
                closed={closed}
                position={{x, y}}
            />,
            'fund-data-grid-cell-edit', this.handleEditClose));
    }

    handleEditClose() {
        const {versionId} = this.props;

        this.dispatch(nodeFormActions.fundSubNodeFormHandleClose(versionId, 'DATA_GRID'));

        this.setState({},
            ()=> {
                ReactDOM.findDOMNode(this.refs.dataGrid).focus()
            }
        )
    }


    /**
     * Otevření uzlu v nové záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInNewTab(row) {
        const {versionId, fund} = this.props;
        this.dispatch(contextMenuHide());

        var parentNode = row.parentNode;
        if (parentNode == null) {   // root
            parentNode = createFundRoot(fund);
        }

        this.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, true, null, true));
    }

    handleFulltextChange(value) {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextClear(versionId))
    }

    handleFulltextSearch(value) {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextSearch(versionId, value, false, null, this.props.fundDataGrid.data))
    }

    handleFulltextPrevItem() {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextPrevItem(versionId))
    }

    handleFulltextNextItem() {
        const {versionId} = this.props;
        this.dispatch(fundDataFulltextNextItem(versionId))
    }

    handleChangeFocus(row, col) {
        const {versionId} = this.props;
        this.dispatch(fundDataChangeCellFocus(versionId, row, col))
    }

    handleChangeRowIndexes(indexes) {
        const {versionId} = this.props;
        this.dispatch(fundDataChangeRowIndexes(versionId, indexes))
    }

    /**
     * Otevření uzlu v záložce.
     * @param row {Object} řádek dat
     */
    handleSelectInTab(row) {
        const {versionId, fund} = this.props;
        this.dispatch(contextMenuHide());

        var parentNode = row.parentNode
        if (parentNode == null) {   // root
            parentNode = createFundRoot(fund);
        }

        this.dispatch(fundSelectSubNode(versionId, row.node.id, parentNode, false, null, true));
    }

    handleToggleExtendedSearch() {
        const {versionId} = this.props
        this.dispatch(fundDataFulltextExtended(versionId))
    }

    handleExtendedSearch = () => {
        this.dispatch(modalDialogShow(this, i18n('search.extended.title'),
            <ArrSearchForm
                onSubmitForm={this.handleExtendedSearchData}
                initialValues={this.props.fundDataGrid.data}
            />
        ));
    };

    handleExtendedSearchData = (result) => {
        const {versionId} = this.props;

        let params = [];

        let text = null;
        switch (result.type) {
            case "FORM": {
                result.condition.forEach((conditionItem, index) => {
                    let param = {};
                    param.type = conditionItem.type;
                    param.value = conditionItem.value;
                    switch (conditionItem.type) {
                        case "TEXT": {
                            param["@class"] = ".TextSearchParam";
                            break;
                        }
                        case "UNITDATE": {
                            param["@class"] = ".UnitdateSearchParam";
                            param.calendarId = parseInt(conditionItem.calendarTypeId);
                            param.condition = conditionItem.condition;
                            break;
                        }
                    }
                    params.push(param);
                });
                break;
            }

            case "TEXT": {
                text = result.text;
                break;
            }
        }

        return this.dispatch(fundDataFulltextSearch(versionId, text, true, params, result));
    };

    render() {
        const {fundId, fund, fundDataGrid, versionId, rulDataTypes, descItemTypes, dispatch, readMode} = this.props;
        const {cols} = this.state;

        // Hledání
        var search = (
            <SearchWithGoto
                itemsCount={fundDataGrid.searchedItems.length}
                selIndex={fundDataGrid.searchedCurrentIndex}
                textAreaInput={fundDataGrid.searchExtended}
                filterText={fundDataGrid.luceneQuery ? i18n('search.extended.label') : fundDataGrid.searchText}
                placeholder={i18n(fundDataGrid.searchExtended ? 'arr.fund.extendedSearch.text' : 'search.input.search')}
                showFilterResult={fundDataGrid.showFilterResult}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextChange={this.handleFulltextChange}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
                extendedSearch
                extendedReadOnly={fundDataGrid.luceneQuery}
                onClickExtendedSearch={this.handleExtendedSearch}
            />
        );

        // ---
        return (
            <div ref='gridContainer' className='fund-datagrid-container-wrap'>
                <div ref='grid' className='fund-datagrid-container'>
                    <div className='actions-container'>
                        <div className="actions-search">
                            {search}
                        </div>
                        <div className="actions-buttons">
                            <Button className="update"
                                disabled={!(fundDataGrid.rowsDirty || fundDataGrid.filterDirty)}
                                onClick={this.handleFilterUpdateData}
                            ><Icon glyph='fa-refresh'/>{i18n('arr.fund.filterSettings.updateData.action')}</Button>
                            <Button onClick={this.handleFilterClearAll}><Icon
                                glyph='fa-trash'/>{i18n('arr.fund.filterSettings.clearAll.action')}</Button>
                            <Button onClick={this.handleColumnSettings}
                                    title={i18n('arr.fund.columnSettings.action')}><Icon glyph='fa-columns'/></Button>
                        </div>
                    </div>
                    <StoreHorizontalLoader store={{
                        isFetching: fundDataGrid.isFetchingData || fundDataGrid.isFetchingFilter || descItemTypes.isFetching || rulDataTypes.isFetching,
                        fetched: fundDataGrid.fetchedData || fundDataGrid.fetchedFilter || descItemTypes.fetched || rulDataTypes.fetched
                    }} />
                    <div className='grid-container'>
                        <DataGrid
                            ref='dataGrid'
                            rows={fundDataGrid.items}
                            cols={cols}
                            focusRow={fundDataGrid.cellFocus.row}
                            focusCol={fundDataGrid.cellFocus.col}
                            selectedIds={fundDataGrid.selectedIds}
                            selectedRowIndexes={fundDataGrid.selectedRowIndexes}
                            onColumnResize={this.handleColumnResize}
                            onChangeFocus={this.handleChangeFocus}
                            onChangeRowIndexes={this.handleChangeRowIndexes}
                            onSelectedIdsChange={this.handleSelectedIdsChange}
                            onContextMenu={this.handleContextMenu}
                            onEdit={this.handleEdit}
                            disabled={readMode}
                            startRowIndex={fundDataGrid.pageSize * fundDataGrid.pageIndex}
                            morePages={getPagesCount(fundDataGrid.itemsCount, fundDataGrid.pageSize) > 1}
                        />
                        <DataGridPagination
                            itemsCount={fundDataGrid.itemsCount}
                            pageSize={fundDataGrid.pageSize}
                            pageIndex={fundDataGrid.pageIndex}
                            onSetPageIndex={pageIndex => {
                                dispatch(fundDataGridSetPageIndex(versionId, pageIndex))
                            }}
                            onChangePageSize={pageSize => {
                                dispatch(fundDataGridSetPageSize(versionId, pageSize))
                            }}
                        />
                    </div>
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {splitter} = state;
    return {
        splitter
    }
}

export default connect(mapStateToProps)(FundDataGrid);
