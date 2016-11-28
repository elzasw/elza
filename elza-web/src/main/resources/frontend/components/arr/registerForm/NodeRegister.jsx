import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete, FormInput} from 'components/index.jsx';
import {decorateValue} from './../nodeForm/DescItemUtils.jsx'
import {objectById} from 'stores/app/utils.jsx'

import {Button} from 'react-bootstrap';



require('./NodeRegister.less');

export default class NodeRegister extends AbstractReactComponent {

    static PropTypes = {
        onChange: React.PropTypes.func.isRequired,
        onCreateRecord: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func.isRequired,
        versionId: React.PropTypes.number,
        item: React.PropTypes.object,
        closed: React.PropTypes.bool,
        value: React.PropTypes.number
    };

    state = {
        recordList: []
    };

    handleChange = (id, record) => {
        this.props.onChange(record);
    };

    handleSearchChange = (text) => {
        WebApi.findRegistry(text == "" ? null : text, null, null, this.props.versionId)
                .then(json => {
                    this.setState({recordList: json.recordList})
                })
    };

    handleCreateRecord = () => {
        this.refs.registryAutocomplete.closeMenu();
        this.props.onCreateRecord();
    };

    handleDetail = (recordId) => {
        this.props.onDetail(recordId);
    };

    renderRecord = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return <div className={cls} key={item.recordId} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>
    };

    renderFooter = () => {
        return <div className="create-record">
            <Button onClick={this.handleCreateRecord}><Icon glyph='fa-plus'/>{i18n('registry.addNewRegistry')}</Button>
        </div>
    };

    render() {
        const {item, closed} = this.props;
        const footer = this.renderFooter();
        const record = item.record ? item.record : null;

        const actions = record ? [<div onClick={this.handleDetail.bind(this, record.recordId)} className='btn btn-default detail'><Icon glyph='fa-user'/></div>] : [];

        return <div className='link-value'>
            <Autocomplete
                key={'registry-autocomplete'}
                ref="registryAutocomplete"
                {...decorateValue(this, item.hasFocus, item.error.value, closed)}
                customFilter
                className='autocomplete-record'
                footer={footer}
                value={record}
                items={this.state.recordList}
                getItemId={(item) => item ? item.recordId : null}
                getItemName={(item) => item ? item.record : ''}
                onSearchChange={this.handleSearchChange}
                onChange={this.handleChange}
                renderItem={this.renderRecord}
                actions={actions}
            />
        </div>
    }
}