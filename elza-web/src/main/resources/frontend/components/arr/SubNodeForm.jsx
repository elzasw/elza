/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require ('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {Glyphicon} from 'react-bootstrap';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormDescItemTypeAdd, faSubNodeFormValueChange, faSubNodeFormDescItemTypeDelete, faSubNodeFormValueChangeSpec,faSubNodeFormValueBlur, faSubNodeFormValueFocus, faSubNodeFormValueAdd, faSubNodeFormValueDelete} from 'actions/arr/subNodeForm'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
var classNames = require('classnames');
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import DescItemString from './nodeForm/DescItemString'
import DescItemType from './nodeForm/DescItemType'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm'

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemGroup', 'handleAddDescItemType', 'renderDescItemType', 'handleChange', 'handleChangeSpec', 'handleDescItemTypeRemove', 'handleBlur', 'handleFocus', 'renderFormActions', 'getDescItemTypeInfo', 'handleDescItemAdd', 'handleDescItemRemove');

//console.log("@@@@@-SubNodeForm-@@@@@", props);
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
//console.log("@@@@@-SubNodeForm-@@@@@", props);
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    renderDescItemGroup(descItemGroup, descItemGroupIndex) {
        var descItemTypes = descItemGroup.descItemTypes.map((descItemType, descItemTypeIndex) => (
            this.renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex)
        ));
        var cls = classNames({
            'desc-item-group': true,
            active: descItemGroup.hasFocus
        });

        return (
            <div className={cls}>
                <div className='desc-item-types'>
                    {descItemTypes}
                </div>
            </div>
        )
    }

    getDescItemTypeInfo(descItemType) {
        return this.props.descItemTypeInfos[indexById(this.props.descItemTypeInfos, descItemType.id)];
    }

    handleDescItemRemove(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        }

        this.dispatch(faSubNodeFormValueDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    handleDescItemTypeRemove(descItemGroupIndex, descItemTypeIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }

        this.dispatch(faSubNodeFormDescItemTypeDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    handleDescItemAdd(descItemGroupIndex, descItemTypeIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }

        this.dispatch(faSubNodeFormValueAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    handleBlur(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueBlur(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    handleFocus(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueFocus(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation));
    }

    handleChange(descItemGroupIndex, descItemTypeIndex, descItemIndex, value) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueChange(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, value));
    }

    handleChangeSpec(descItemGroupIndex, descItemTypeIndex, descItemIndex, value) {
        var valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex
        }

        this.dispatch(faSubNodeFormValueChangeSpec(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, valueLocation, value));
    }

    renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex) {
        var rulDataType = this.props.rulDataTypes.items[indexById(this.props.rulDataTypes.items, descItemType.dataTypeId)];
        var descItemTypeInfo = this.getDescItemTypeInfo(descItemType);

        return (
            <DescItemType
                descItemType={descItemType}
                descItemTypeInfo={descItemTypeInfo}
                rulDataType={rulDataType}
                calendarTypes={this.props.calendarTypes}
                onDescItemAdd={this.handleDescItemAdd.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemRemove={this.handleDescItemRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChange={this.handleChange.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onChangeSpec={this.handleChangeSpec.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onBlur={this.handleBlur.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onFocus={this.handleFocus.bind(this, descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
            />
        )
    }

    handleAddDescItemType() {
        // Pro přidání chceme jen ty, které zatím ještě nemáme
        var descItemTypesMap = {};
        this.props.descItemTypeInfos.forEach(descItemType => {
            descItemTypesMap[descItemType.id] = descItemType;
        })
        this.props.formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(descItemType => {
                delete descItemTypesMap[descItemType.id];
            })
        })
        var descItemTypes = [];
        Object.keys(descItemTypesMap).forEach(function (key) {
            descItemTypes.push(descItemTypesMap[key]);
        });       

        // Seřazení podle position
        descItemTypes.sort((a, b) => a.position - b.position);

        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmit={(data) => {
            this.dispatch(modalDialogHide());
            this.dispatch(faSubNodeFormDescItemTypeAdd(this.props.versionId, this.props.selectedSubNodeId, this.props.nodeKey, data.descItemTypeId));
        }} />
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    renderFormActions() {
        return (
            <div className='node-form-actions'>
                <NoFocusButton onClick={this.handleAddDescItemType}><Glyphicon glyph="plus" />Přidat prvek</NoFocusButton>
                <NoFocusButton><Glyphicon glyph="lock" />Odemknout vše</NoFocusButton>
                <NoFocusButton><Glyphicon glyph="plus" />Přidat JP před</NoFocusButton>
                <NoFocusButton><Glyphicon glyph="plus" />Přidat JP za</NoFocusButton>
                <NoFocusButton><Glyphicon glyph="list" />Rejstříky</NoFocusButton>
                <NoFocusButton><Glyphicon glyph="remove" />Zrušit JP</NoFocusButton>
            </div>
        )
    }

    render() {
        if (this.props.calendarTypes.isFetching && !this.props.calendarTypes.fetched) {
            return <div className='node-form'></div>
        }

        var formActions = this.renderFormActions();
        var descItemGroups = this.props.formData.descItemGroups.map((group, groupIndex) => (
            this.renderDescItemGroup(group, groupIndex)
        ));

        return (
            <div className='node-form'>
                {formActions}
                <div className='desc-item-groups'>
                    {descItemGroups}
                </div>
            </div>
        )
    }
}

module.exports = connect()(SubNodeForm);

