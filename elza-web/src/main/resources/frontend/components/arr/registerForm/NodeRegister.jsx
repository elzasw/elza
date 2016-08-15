import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {decorateValue} from './../nodeForm/DescItemUtils.jsx'

import {MenuItem, Button} from 'react-bootstrap';

require('./NodeRegister.less')

export default class NodeRegister extends AbstractReactComponent {
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

        WebApi.findRegistry(text, null, null, this.props.versionId)
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
                    <Button onClick={this.handleCreateRecord}><Icon glyph='fa-plus'/>{i18n('registry.addNewRegistry')}</Button>
                </div>
        )
    }

    render() {
        const {item, closed} = this.props;
        var footer = this.renderFooter();
        var value = item.record ? {id: item.record.recordId, name: item.record.record} : null;

        const actions = [];

        if (item.record) {
            actions.push(<div onClick={this.handleDetail.bind(this, item.record.recordId)} className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
        }

        return (
                <div className='link-value'>
                    <Autocomplete
                            {...decorateValue(this, item.hasFocus, item.error.value, closed)}
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
                            actions={[actions]}
                            />
                </div>
        )
    }
}