import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'

import './RegistryField.less'

class RegistryField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        registryParent: null,
        registerTypeId: null,
        roleTypeId: null,
        partyId: null,
        versionId: null,
    };

    static PropTypes = {
        detail: React.PropTypes.bool.isRequired,
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func,
        onCreate: React.PropTypes.func.isRequired,
        registryParent: React.PropTypes.number,
        registerTypeId: React.PropTypes.number,
        roleTypeId: React.PropTypes.number,
        partyId: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    state = {registryList: []};


    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {
        text = text == "" ? null : text;
        const {roleTypeId, partyId, registryParent, registerTypeId, versionId} = this.props;
        console.log(roleTypeId, partyId)
        let promise = null;
        if (roleTypeId || partyId) {
            promise = WebApi.findRecordForRelation(text, roleTypeId, partyId);
        } else {
            promise = WebApi.findRegistry(text, registryParent, registerTypeId, versionId);
        }
        promise.then(json => {
            this.setState({
                registryList: json.recordList
            })
        })
    };

    renderRecord = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return <div className={cls} key={item.id} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>;
    };

    normalizeValue = (call) => (obj,id) => {
        // změna typu aby se objekt dal použít jako návazný
        const newobj = {
            ...obj,
            '@class': 'cz.tacr.elza.controller.vo.RegRecordVO',
        };
        call(newobj, id);
    };

    render() {
        const {locked, onChange, onBlur, ...otherProps} = this.props;

        return <div className='desc-item-value desc-item-value-parts'>
            <Autocomplete
                ref='autocomplete'
                customFilter
                items={this.state.registryList}
                getItemId={(item) => item ? item.id : null}
                getItemName={(item) => item && item.record ? item.record : ''}
                onSearchChange={this.handleSearchChange}
                renderItem={this.renderRecord}
                //actions={[actions]}
                onChange={this.normalizeValue(onChange)}
                onBlur={this.normalizeValue(onBlur)}
                {...otherProps}
            />
        </div>;
    }
}

export default connect(null, null, null, { withRef: true })(RegistryField);
