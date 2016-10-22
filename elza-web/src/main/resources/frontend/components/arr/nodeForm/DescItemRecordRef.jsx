require ('./DescItemRecordRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, Button} from 'react-bootstrap';
import * as perms from 'actions/user/Permission.jsx';
import DescItemLabel from './DescItemLabel.jsx'

var DescItemRecordRef = class DescItemRecordRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange', 'renderRecord', 'handleSearchChange',
            'renderFooter', 'handleCreateRecord', 'focus');

        this.state = {recordList: []};
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    handleChange(id, valueObj) {
        this.props.onChange(valueObj);
    }

    handleSearchChange(text) {

        text = text == "" ? null : text;

        WebApi.findRegistry(text, null, null, this.props.versionId)
                .then(json => {
                    this.setState({
                        recordList: json.recordList.map(record => {
                            return record
                        })
                    })
                })
    }

    handleCreateRecord() {
        this.refs.autocomplete.closeMenu();
        this.props.onCreateRecord();
    }

    handleDetail(recordId) {
        this.props.onDetail(recordId);
    }

    renderRecord(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
                <div className={cls} key={item.recordId} >
                    <div className="name" title={item.record}>{item.record}</div>
                    <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
                </div>
        )
    }

    renderFooter() {
        return (
            <div className="create-record">
                <Button onClick={this.handleCreateRecord}><Icon glyph='fa-plus'/>{i18n('registry.addNewRegistry')}</Button>
            </div>
        )
    }

    render() {
        const {userDetail, descItem, locked, singleDescItemTypeEdit, readMode, cal} = this.props;
        var value = descItem.record ? descItem.record : null;

        if (readMode) {
            if (value) {
                return (
                    <DescItemLabel onClick={this.handleDetail.bind(this, descItem.record.recordId)} value={value.record} />
                )
            } else {
                return (
                    <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} />
                )
            }
        }

        var footer
        if (!singleDescItemTypeEdit) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
                footer = this.renderFooter();
            }
        }

        var actions = new Array;
        if (descItem.record) {
            if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: descItem.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, descItem.record.recordId)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            }
        }

        return (
                <div className='desc-item-value desc-item-value-parts'>
                    <Autocomplete
                            {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-record'])}
                            ref='autocomplete'
                            customFilter
                            footer={footer}
                            value={value}
                            items={this.state.recordList}
                            getItemId={(item) => item ? item.recordId : null}
                            getItemName={(item) => item ? item.record : ''}
                            onSearchChange={this.handleSearchChange}
                            onChange={this.handleChange}
                            renderItem={this.renderRecord}
                            actions={[actions]}
                            />
                </div>
        )
    }
}

function mapStateToProps(state) {
    const {userDetail} = state
    return {
        userDetail,
    }
}

module.exports = connect(mapStateToProps, null, null, { withRef: true })(DescItemRecordRef);
