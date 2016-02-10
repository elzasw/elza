require ('./DescItemRecordRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions'
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils'

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
                    <Button onClick={this.handleCreateRecord}>{i18n('registry.addNewRegistry')}</Button>
                </div>
        )
    }

    render() {
        const {descItem, locked} = this.props;
        var footer = this.renderFooter();
        var value = descItem.record ? descItem.record : null;

        var actions = new Array;

        if (descItem.record) {
            actions.push(<div onClick={this.handleDetail.bind(this, descItem.record.recordId)} className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
        }

        return (
                <div className='desc-item-value desc-item-value-parts'>
                    <Autocomplete
                            {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-record'])}
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

module.exports = connect()(DescItemRecordRef);
