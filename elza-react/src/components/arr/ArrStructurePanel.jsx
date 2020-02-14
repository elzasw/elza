import React from 'react';
import {AbstractReactComponent, i18n, Icon, Loading, CheckListBox, FormInput, TooltipTrigger} from 'components/shared';
import FloatingMenu from "components/shared/floating-menu/FloatingMenu.jsx";
import {objectById} from "shared/utils";
import {Button, DropdownButton, FormControl, MenuItem} from 'react-bootstrap';
import {connect} from 'react-redux';
import {WebApi} from "../../actions/WebApi";
import {
    structureTypeFetchIfNeeded, AREA, structureTypeFilter,
    structureTypeInvalidate,
    DEFAULT_STRUCTURE_TYPE_MAX_SIZE
} from "../../actions/arr/structureType";
import storeFromArea from "../../shared/utils/storeFromArea";
import './ArrStructurePanel.scss'
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AddStructureDataForm from "./structure/AddStructureDataForm";
import UpdateStructureDataForm from "./structure/UpdateStrucutreDataForm";
import StructureExtensionsForm from "./structure/StructureExtensionsForm";
import PropTypes from 'prop-types';
import UpdateMultipleSub from "./structure/UpdateMultipleSub";
import {addToastrWarning} from "../shared/toastr/ToastrActions";
import DescItemFactory from "components/arr/nodeForm/DescItemFactory.jsx";
import ListPager from "components/shared/listPager/ListPager";


class ArrStructurePanel extends AbstractReactComponent {
    static propTypes = {
        code: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        id: PropTypes.number.isRequired,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        maxSize: PropTypes.number
    };

    static defaultProps = {
        maxSize: DEFAULT_STRUCTURE_TYPE_MAX_SIZE
    };

    state = {
        checkedIndexes: [],
        contextMenu: {
            isOpen: false,
            coordinates: {x:0,y:0}
        },
        multiselect: false
    };

    componentDidMount() {
        const {store:{filter}} = this.props;
        this.fetchIfNeeded();
        // set default filter to "all"
        if(!filter || typeof filter.assignable === "undefined"){
            this.filter({
                ...filter,
                assignable: ""
            });
        }
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {code, fundVersionId} = props;
        this.props.dispatch(structureTypeFetchIfNeeded({code, fundVersionId}));
    };

    handleExtensionsSettings = () => {
        const {fundVersionId, code, name} = this.props;
        WebApi.findFundStructureExtension(fundVersionId, code).then(extensions => {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.settings.title", name), <StructureExtensionsForm
                initialValues={{extensions}}
                onSubmit={(data) => WebApi.updateFundStructureExtension(fundVersionId, code, data.extensions.filter(i => i.active).map(i => i.code))}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                }}
            />));
        })
    };

    handleChangeSelection = (checkedIndexes) => {
        this.setState({ checkedIndexes });
    };

    handleCreate = () => {
        const {code, fundVersionId, fundId, name} = this.props;
        WebApi.createStructureData(fundVersionId, code).then(structureData => {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.add.title", name), <AddStructureDataForm
                fundId={fundId}
                fundVersionId={fundVersionId}
                structureData={structureData}
                descItemFactory={DescItemFactory}
                onSubmit={() => WebApi.confirmStructureData(fundVersionId, structureData.id)}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                    this.props.dispatch(structureTypeInvalidate());
                }}
            />, "", () => WebApi.deleteStructureData(fundVersionId, structureData.id)))
        });
    };

    handleCreateMulti = () => {
        const {code, fundVersionId, fundId, name} = this.props;
        WebApi.createStructureData(fundVersionId, code).then(structureData => {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.addMultiple.title", name), <AddStructureDataForm
                multiple
                fundId={fundId}
                fundVersionId={fundVersionId}
                structureData={structureData}
                descItemFactory={DescItemFactory}
                onSubmit={(data) => WebApi.duplicateStructureDataBatch(fundVersionId, structureData.id, data)}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                    this.props.dispatch(structureTypeInvalidate());
                }}
            />, "", () => WebApi.deleteStructureData(fundVersionId, structureData.id)))
        });
    };

    /**
     * Vrátí aktuální výběr
     *
     *
     */
    getActiveSelection = (clickedItem = null) => {
        const {multiselect, checkedIndexes} = this.state;

        if (checkedIndexes) {
            const {store: {rows}} = this.props;
            if (checkedIndexes.length === 1) { // Vybrána pouze 1 položka
                return [rows[parseInt(checkedIndexes[0])].id];
            } else if (checkedIndexes.length > 1) { // Vybráno více položek
                return checkedIndexes.map(i => rows[i].id);
            } else { // Vybráno 0 položek
                console.warn("Invalid state");
                return null;
            }
        }

        console.warn("Invalid state");
        return null;
    };

    handleUpdate = (structureData) => {
        const {fundVersionId, fundId, name, code, readMode} = this.props;

        let structureDataIds = this.getActiveSelection(structureData);
        if ((!structureDataIds || structureDataIds.length === 0) && structureData && structureData.id) {
            structureDataIds = [structureData.id];
        }

        const title = i18n(readMode ? "arr.structure.modal.show.title" : "arr.structure.modal.update.title", name);

        if (structureDataIds.length === 1) {
            this.props.dispatch(modalDialogShow(this, title, <UpdateStructureDataForm
                descItemFactory={DescItemFactory}
                fundId={fundId}
                readMode={readMode}
                fundVersionId={fundVersionId}
                id={structureDataIds[0]}
            />));
        } else if (structureDataIds.length > 1 && !readMode) {
            this.props.dispatch(modalDialogShow(this, title, <UpdateMultipleSub
                descItemFactory={DescItemFactory}
                onSubmit={(data) => WebApi.updateStructureDataBatch(fundVersionId, code, data)}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                    this.props.dispatch(structureTypeInvalidate());
                }}
                fundVersionId={fundVersionId}
                initialValues={{
                    autoincrementItemTypeIds: [],
                    deleteItemTypeIds: [],
                    items: {},
                    structureDataIds
                }}
                id={structureDataIds[0]}
            />));
        } else if (readMode) {
            this.props.dispatch(addToastrWarning(i18n("arr.structure.modal.noshow")));
        }
        this.closeContextMenu();
    };

    filter = (toFilter) => {
        const {store:{filter}} = this.props;
        this.props.dispatch(structureTypeFilter({...filter, ...toFilter}));
    };

    handleFilterPrev = () => {
        const { filter } = this.props.store;
        let { from } = filter;

        if (from >= DEFAULT_STRUCTURE_TYPE_MAX_SIZE) {
            from = from - DEFAULT_STRUCTURE_TYPE_MAX_SIZE;
            this.props.dispatch(structureTypeFilter({...filter, from}));
        }
    };

    handleFilterNext = () => {
        const { filter, count } = this.props.store;
        let { from } = filter;

        if (from < count - DEFAULT_STRUCTURE_TYPE_MAX_SIZE) {
            from = from + DEFAULT_STRUCTURE_TYPE_MAX_SIZE;
            this.props.dispatch(structureTypeFilter({...filter, from}));
        }
    };

    /**
     *
     * @param clickItem
     * @param {Boolean} assignableState
     */
    handleSetAssignable = (clickItem, assignableState) => {
        const {fundVersionId} = this.props;
        const selection = this.getActiveSelection(clickItem);
        const ids = selection ? this.getActiveSelection(clickItem) : [clickItem.id];

        WebApi.setAssignableStructureDataList(fundVersionId, assignableState, ids).then(() => {
            this.props.dispatch(structureTypeInvalidate());
        });
        this.closeContextMenu();
    };

    deleteItem = (itemId) => {
        const {fundVersionId} = this.props;
        WebApi.deleteStructureData(fundVersionId, itemId).then(() => {
            this.props.dispatch(structureTypeInvalidate());
        });
    };

    handleDelete = (clickItem) => {
        const ids = this.getActiveSelection(clickItem);

        if (ids) {
            ids.forEach((id) => this.deleteItem(id));
        } else {
            this.deleteItem(clickItem.id);
        }

        this.closeContextMenu();
    };

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    openContextMenu = (node, e) => {
        e.stopPropagation();
        e.preventDefault();
        this.setState({
            contextMenu:{
                isOpen:true,
                coordinates: {
                    x: e.clientX,
                    y: e.clientY
                },
                node,
                focusable: e.keyCode == 13
            }
        });
    }

    closeContextMenu = () => {
        this.setState({
            contextMenu:{
                ...this.state.contextMenu,
                isOpen:false,
                focusable: false
            }
        });
        if (this.list) {
            this.list.focus();
        }
    }

    handleKeyMenu = (e, call) => {
        if (e.keyCode == 13) {
            call && call();
        } else if (e.keyCode == 27) {
            this.closeContextMenu();
        } else {
            switch (e.key) {
                case "ArrowDown":
                    this.focusOnItem(1);
                    break;
                case "ArrowUp":
                    this.focusOnItem(-1);
                    break;
            }
        }
    }

    focusOnItem = (diff) => {
        if (this.menu) {
            const menu = this.menu.menu;
            let curr = null;
            let poss = [];
            for (let i = 0; i < menu.children.length; i++) {
                const item = menu.children[i];
                if (item.tabIndex >= 0) {
                    poss.push(i);
                }
                if (item === document.activeElement) {
                    curr = poss.length - 1;
                }
            }
            const newIndex = curr + diff;
            if (newIndex >= 0 && poss.length > newIndex) {
                menu.children[poss[newIndex]].focus();
            }
        }
    }

    renderContextMenu = () => {
        const { filter } = this.props.store;
        const {coordinates, node, focusable} = this.state.contextMenu;
        const {readMode} = this.props;

        const menuParts = [];

        if (readMode) {
            menuParts.push(<div key="show" tabIndex={0} className="item" onKeyDown={e => this.handleKeyMenu(e, this.handleUpdate.bind(this, node))} onClick={this.handleUpdate.bind(this, node)}>{i18n("arr.structure.item.contextMenu.show")}</div>);
        } else {
            if (node.assignable) {
                menuParts.push(<div key="changeToClosed" tabIndex={0} className="item" onKeyDown={e => this.handleKeyMenu(e, this.handleSetAssignable.bind(this, node, false))} onClick={this.handleSetAssignable.bind(this, node, false)}>{i18n("arr.structure.item.contextMenu.changeToClosed")}</div>);
            } else {
                menuParts.push(<div key="changeToOpen" tabIndex={0} className="item" onKeyDown={e => this.handleKeyMenu(e, this.handleSetAssignable.bind(this, node, true))} onClick={this.handleSetAssignable.bind(this, node, true)}>{i18n("arr.structure.item.contextMenu.changeToOpen")}</div>);
            }
            menuParts.push(<div key="d1"  className="divider" />);
            menuParts.push(<div key="update" tabIndex={0} className="item" onKeyDown={e => this.handleKeyMenu(e, this.handleUpdate.bind(this, node))} onClick={this.handleUpdate.bind(this, node)}>{i18n("arr.structure.item.contextMenu.update")}</div>);
            menuParts.push(<div key="d2" className="divider" />);
            menuParts.push(<div key="delete" tabIndex={0} className="item" onKeyDown={e => this.handleKeyMenu(e, this.handleDelete.bind(this, node))} onClick={this.handleDelete.bind(this, node)}>{i18n("arr.structure.item.contextMenu.delete")}</div>);
        }
        return (
            <FloatingMenu ref={(ref)=>{this.menu = ref;}} coordinates={coordinates} closeMenu={this.closeContextMenu} focusable={focusable}>
                {menuParts}
            </FloatingMenu>
        );
    }

    renderErrorContent = (error) => {
        const {descItemTypes} = this.props;
        /*
        const exampleError = {
            emptyValue: true,
            duplicateValue: true,
            impossibleItemTypeIds: ["25", "2", "3"],
            requiredItemTypeIds: ["1"]
        };
        */
        let parts = [];
        error = JSON.parse(error);
        if(error.emptyValue){
            parts.push(<div key="empty" className="error-item">{i18n("arr.structure.item.error.emptyValue")}</div>);
        }
        if(error.duplicateValue){
            parts.push(<div key="duplicate" className="error-item">{i18n("arr.structure.item.error.duplicateValue")}</div>);
        }
        if(error.impossibleItemTypeIds.length > 0){
            const items = [];
            error.impossibleItemTypeIds.map((id, index)=>{
                const descItem = objectById(descItemTypes, id);
                items.push(<li key={index}>{descItem.name}</li>);
            });
            parts.push(
                <div key="items" className="error-list error-item">
                  <div>{i18n("arr.structure.item.error.impossibleItemTypes")}</div>
                  <ul>{items}</ul>
                </div>
            );
        }
        if(error.requiredItemTypeIds.length > 0){
            const items = [];
            error.requiredItemTypeIds.map((id)=>{
                const descItem = objectById(descItemTypes, id);
                items.push(<li>{descItem.name}</li>);
            });
            parts.push(
                <div className="error-list error-item">
                  <div>{i18n("arr.structure.item.error.requiredItemTypes")}</div>
                  <ul>{items}</ul>
                </div>
            );
        }
        return <div>{parts}</div>;
    }

    renderItemContent = (props) => {
        const {item, active, index, ...otherProps} = props;

        const hasError = item.state === 'ERROR' && item.errorDescription;
        const {complement} = item;

        return (
            <div {...otherProps} key={index} onContextMenu={this.openContextMenu.bind(this, item)}>
                <div className="structure-name">
                    {item.value || <em key="no-val">{i18n("arr.structure.list.item.noValue")}</em>}
                    {complement &&
                    <div key="complement" className="structure-name-complement">
                        {complement}
                    </div>
                    }
                </div>
                {
                    hasError &&
                    <TooltipTrigger key="tooltip" tooltipClass="error-message" content={this.renderErrorContent(item.errorDescription)} placement="left">
                        <Icon glyph="fa-exclamation-triangle" />
                    </TooltipTrigger>
                }
                <Button className="btn--context-menu" bsStyle="default" onClick={this.openContextMenu.bind(this, item)}>
                    <Icon glyph="fa-ellipsis-v" />
                </Button>
            </div>
        )
    }

    handleMultiselect = () => {
        const {multiselect} = this.state;
        this.setState({multiselect: !multiselect});
    }

    render() {
        const {rows, filter, fetched, count} = this.props.store;
        const {readMode, maxSize} = this.props;
        const {checkedIndexes, contextMenu, multiselect} = this.state;

        if (!fetched) {
            return <Loading />
        }

        return <div className={"arr-structure-panel"}>
            {!readMode && <div className="actions">
                <DropdownButton bsStyle="default" title={<Icon glyph="fa-plus-circle" />} noCaret id="arr-structure-panel-add">
                    <MenuItem eventKey="1" onClick={this.handleCreate}>{i18n("arr.structure.addOne")}</MenuItem>
                    <MenuItem eventKey="2" onClick={this.handleCreateMulti}>{i18n("arr.structure.addMany")}</MenuItem>
                </DropdownButton>
                <Button  className="btn--multiselect" bsStyle="default" onClick={this.handleMultiselect}>
                    <Icon glyph={multiselect ? "fa-check-square" : "fa-check-square-o"} />
                </Button>
                {multiselect &&
                    <Button bsStyle="default" onClick={(e) => this.openContextMenu(null, e)} disabled={!checkedIndexes || checkedIndexes.length < 1 || rows.length < 1 }>
                        <Icon glyph="fa-bars" />
                    </Button>
                }
                <Button bsStyle="default" onClick={this.handleExtensionsSettings} className={"pull-right"}>
                    <Icon glyph="fa-cogs" />
                </Button>
            </div>}
            <div className="filter flex">
                <div>
                    <FormControl componentClass={"select"} name={"assignable"} onChange={({target: {value}}) => this.filter({assignable:value})} value={filter.assignable}>
                        <option value={""}>{i18n("arr.structure.filter.assignable.all")}</option>
                        <option value={true}>{i18n("arr.structure.filter.assignable.true")}</option>
                        <option value={false}>{i18n("arr.structure.filter.assignable.false")}</option>
                    </FormControl>
                </div>
                <FormInput className="text-filter" name={"text"} type="text" onChange={({target: {value}}) => this.filter({text:value})} value={filter.text} placeholder={i18n("arr.structure.filter.text.placholder")}/>
            </div>
            {rows && rows.length > 0
                ? <CheckListBox
                    ref={(ref)=>{this.list = ref;}}
                    tabindex={0}
                    className="list"
                    key="list"
                    items={rows}
                    filter={filter}
                    onSelect={(item, itemIndex, e) => this.openContextMenu(item, e)}
                    onChangeSelection={this.handleChangeSelection}
                    renderItemContent={this.renderItemContent}
                    multiselect={multiselect}
                />
                : <div key="no-result" className="list listbox-wrapper no-result text-center">{i18n('search.action.noResult')}</div>
            }
            {contextMenu.isOpen && this.renderContextMenu()}
            {count > maxSize &&
                <ListPager
                    key="pager"
                    prev={this.handleFilterPrev}
                    next={this.handleFilterNext}
                    from={filter.from}
                    maxSize={maxSize}
                    totalCount={count}
                />
            }
        </div>
    }
}

export default connect((state, props) => {
    return {
        store: storeFromArea(state, AREA),
        descItemTypes: state.refTables.descItemTypes.items
    }
})(ArrStructurePanel);

