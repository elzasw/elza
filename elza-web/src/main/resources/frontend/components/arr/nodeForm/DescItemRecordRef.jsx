require ('./DescItemRecordRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions'
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

import {MenuItem, Button} from 'react-bootstrap';

var DescItemRecordRef = class DescItemRecordRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange', 'renderRecord', 'handleSearchChange', 'renderFooter', 'handleCreateRecord');

        this.state = {recordList: []};
    }

    componentDidMount() {
    }

    handleChange(id, valueObj) {
        this.props.onChange(id);
    }

    handleSearchChange(text) {

        text = text == "" ? null : text;

        WebApi.findRegistry(text)
                .then(json => {
                    this.setState({
                        recordList: json.recordList.map(record => {
                            return {id: record.recordId, name: record.record, characteristics: record.characteristics, parents: record.parents}
                        })
                    })
                })
    }

    handleCreateRecord() {
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
                <div className={cls} key={item.id} >
                    <div className="name" title={item.name}>{item.name}</div>
                    <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
                </div>
        )
    }

    renderFooter() {
        return (
                <div className="create-record">
                    <Button onClick={this.handleCreateRecord}>{i18n('registry.addNewRegistry')}</Button>
                </div>
        )
    }

    render() {
        const {descItem, locked} = this.props;
        var footer = this.renderFooter();
        var value = descItem.record ? {id: descItem.record.recordId, name: descItem.record.record} : null;

        var actions = new Array;

        if (descItem.record) {
            actions.push(<div onClick={this.handleDetail.bind(this, descItem.record.recordId)} className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
        }

        return (
                <div className='desc-item-value desc-item-value-parts'>
                    <Autocomplete
                            {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                            customFilter
                            className='autocomplete-record'
                            footer={footer}
                            value={value}
                            items={this.state.recordList}
                            getItemId={(item) => item ? item.id : null}
                            getItemName={(item) => item ? item.name : ''}
                            onSearchChange={this.handleSearchChange}
                            onChange={this.handleChange}
                            renderItem={this.renderRecord}
                            />
                </div>
        )
    }
}

module.exports = connect()(DescItemRecordRef);
