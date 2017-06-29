import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent} from 'components/shared';
import {decorateAutocompleteValue} from './../nodeForm/DescItemUtils.jsx'
import {objectById} from 'stores/app/utils.jsx'

import './NodeRegister.less';
import RegistryField from "../../registry/RegistryField";

class NodeRegister extends AbstractReactComponent {

    static PropTypes = {
        onChange: React.PropTypes.func.isRequired,
        onCreateRecord: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func.isRequired,
        versionId: React.PropTypes.number,
        item: React.PropTypes.object,
        closed: React.PropTypes.bool,
        value: React.PropTypes.number
    };

    render() {
        const {item, closed, ...otherProps} = this.props;
        const record = item.record ? item.record : null;


        return <div className='link-value'>
            <RegistryField
                ref="registryField"
                {...otherProps}
                value={record}
                footer={true}
                detail={true}
                {...decorateAutocompleteValue(this, item.hasFocus, item.error.value, closed, ['autocomplete-record'])}
            />
        </div>
    }
}

export default NodeRegister
