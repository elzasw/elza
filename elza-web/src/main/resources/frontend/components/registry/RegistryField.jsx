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
        versionId: React.PropTypes.number
    };

    state = {registryList: []};


    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {
        text = text == "" ? null : text;
        WebApi.findRegistry(text, this.props.registryParent, this.props.registerTypeId, this.props.versionId).then(json => {
            this.setState({
                registryList: json.rows
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

        return <div className={cls} key={item.recordId} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>;
    };

    render() {
        const {locked, ...otherProps} = this.props;

        return <div className='desc-item-value desc-item-value-parts'>
            <Autocomplete
                ref='autocomplete'
                customFilter
                items={this.state.registryList}
                getItemId={(item) => item ? item.id : null}
                getItemName={(item) => item && item.record ? item.record : ''}
                onSearchChange={this.handleSearchChange}
                onChange={this.handleChange}
                onBlur={this.handleBlur}
                renderItem={this.renderRecord}
                //actions={[actions]}
                {...otherProps}
            />
        </div>;
    }
}

export default connect(null, null, null, { withRef: true })(RegistryField);
