import React from 'react';
import {AbstractReactComponent, i18n, Icon, Loading, ListBox, FormInput} from 'components/shared';
import {Button, DropdownButton, FormControl, MenuItem} from 'react-bootstrap';
import {connect} from 'react-redux';
import {WebApi} from "../../actions/WebApi";
import {
    structureTypeFetchIfNeeded, AREA, structureTypeFilter,
    structureTypeInvalidate
} from "../../actions/arr/structureType";
import storeFromArea from "../../shared/utils/storeFromArea";
import debounce from "../../shared/utils/debounce";

import './ArrStructurePanel.less'
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AddStructureDataForm from "./structure/AddStructureDataForm";
import UpdateStructureDataForm from "./structure/UpdateStrucutreDataForm";
import {contextMenuShow, contextMenuHide} from "../../actions/global/contextMenu";
import StructureExtensionsForm from "./structure/StructureExtensionsForm";
import PropTypes from 'prop-types';
import UpdateMultipleSub from "./structure/UpdateMultipleSub";

class ArrStructurePanel extends AbstractReactComponent {

    static propTypes = {
        code: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        id: PropTypes.number.isRequired,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
    };

    state = {activeIndexes: []};

    componentDidMount() {
        this.fetchIfNeeded();
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

    handleChangeSelection = (activeIndexes) => {
        this.setState({activeIndexes});
    };

    handleCreate = () => {
        const {code, fundVersionId, fundId, name} = this.props;
        WebApi.createStructureData(fundVersionId, code).then(structureData => {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.add.title", name), <AddStructureDataForm
                fundId={fundId}
                fundVersionId={fundVersionId}
                structureData={structureData}
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
        const {activeIndexes} = this.state;

        if (!activeIndexes && !clickedItem && clickedItem.id) {
            this.props.dispatch(contextMenuHide());
            return [clickedItem.id];
        } else if (activeIndexes) {
            const {store: {rows}} = this.props;
            if (activeIndexes.length === 1) { // Vybrána pouze 1 položka
                return [rows[parseInt(activeIndexes[0])].id];
            } else if (activeIndexes.length > 1) { // Vybráno více položek
                return activeIndexes.map(i => rows[i].id);
            } else { // Vybráno 0 položek
                console.warn("Invalid state");
                return null;
            }
        }

        console.warn("Invalid state");
        return null;
    };

    handleUpdate = (structureData) => {
        const {fundVersionId, fundId, name, code} = this.props;

        let structureDataIds = this.getActiveSelection(structureData);
        if ((!structureDataIds || structureDataIds.length === 0) && structureData && structureData.id) {
            structureDataIds = [structureData.id];
        }

        if (structureDataIds.length === 1) {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.update.title", name), <UpdateStructureDataForm
                fundId={fundId}
                fundVersionId={fundVersionId}
                id={structureDataIds[0]}
            />));
        } else if (structureDataIds.length > 1) {
            this.props.dispatch(modalDialogShow(this, i18n("arr.structure.modal.update.title", name), <UpdateMultipleSub
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
        }
    };

    filter = (toFilter) => {
        this.props.dispatch(structureTypeFilter({...this.props.filter, ...toFilter}));
    };

    /**
     *
     * @param clickItem
     * @param {Boolean} assignableState
     */
    handleSetAssignable = (clickItem, assignableState) => {
        const {fundVersionId} = this.props;
        const ids = this.getActiveSelection(clickItem);
        this.props.dispatch(contextMenuHide());
        WebApi.setAssignableStructureDataList(fundVersionId, assignableState, ids).then(() => {
            this.props.dispatch(structureTypeInvalidate());
        });
    };

    handleDelete = ({id}) => {
        const {fundVersionId} = this.props;
        this.props.dispatch(contextMenuHide());
        WebApi.deleteStructureData(fundVersionId, id).then(() => {
            this.props.dispatch(structureTypeInvalidate());
        });
    };

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu = (node, e) => {
        e.preventDefault();
        e.stopPropagation();

        const menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSetAssignable.bind(this, node, true)}>{i18n("arr.structure.item.contextMenu.changeToOpen")}</MenuItem>
                <MenuItem onClick={this.handleSetAssignable.bind(this, node, true)}>{i18n("arr.structure.item.contextMenu.changeToClosed")}</MenuItem>
                <MenuItem divider />
                <MenuItem onClick={this.handleUpdate.bind(this, node)}>{i18n("arr.structure.item.contextMenu.update")}</MenuItem>
                <MenuItem divider />
                <MenuItem onClick={this.handleDelete.bind(this, node)}>{i18n("arr.structure.item.contextMenu.delete")}</MenuItem>
            </ul>
        );

        this.props.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    };

    render() {
        const {rows, filter, fetched} = this.props.store;
        const {activeIndexes} = this.state;
        if (!fetched) {
            return <Loading />
        }

        return <div className={"arr-structure-panel"}>
            <div className="actions">
                <DropdownButton bsStyle="default" title={<Icon glyph="fa-plus-circle" />} noCaret id="arr-structure-panel-add">
                    <MenuItem eventKey="1" onClick={this.handleCreate}>{i18n("arr.structure.addOne")}</MenuItem>
                    <MenuItem eventKey="2" onClick={this.handleCreateMulti}>{i18n("arr.structure.addMany")}</MenuItem>
                </DropdownButton>
                <Button bsStyle="default" onClick={this.handleUpdate} disabled={activeIndexes.length < 1}>
                    <Icon glyph="fa-edit" />
                </Button>
                <Button bsStyle="default" onClick={this.handleExtensionsSettings} className={"pull-right"}>
                    <Icon glyph="fa-cogs" />
                </Button>
            </div>
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
            {rows && rows.length > 0 ? <ListBox
                className="list"
                items={rows}
                onChangeSelection={this.handleChangeSelection}
                renderItemContent={i => {
                    let title = i.errorDescription ? i.errorDescription : null;
                    return <div onContextMenu={this.handleContextMenu.bind(this, i)} title={title}>
                        {i.value || <em>{i18n("arr.structure.list.item.noValue")}</em>} {
                            i.state === 'ERROR' && i.errorDescription && <Icon glyph="fa-exclamation-triangle pull-right" />
                        }
                    </div>
                }}
                multiselect={true}
            /> : <div className="list listbox-wrapper no-result text-center">{i18n('search.action.noResult')}</div>}
        </div>


    }
}

export default connect((state, props) => {
    return {
        store: storeFromArea(state, AREA)
    }
})(ArrStructurePanel);

