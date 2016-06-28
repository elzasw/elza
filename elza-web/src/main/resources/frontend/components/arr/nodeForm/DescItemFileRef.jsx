//require ('./DescItemFileRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete} from 'components/index.jsx';
import {decorateAutocompleteValue} from './DescItemUtils.jsx'

var DescItemFileRef = class DescItemFileRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange', 'handleSearchChange', 'focus');

        this.state = {fileList: []};
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    handleChange(id, valueObj) {
        this.props.onChange(valueObj);
    }

    handleSearchChange(text) {

        text = text == "" ? null : text;

        WebApi.findFundFiles(this.props.fundId, text).then(json => {
            this.setState({
                fileList: json.list
            })
        })
    }

    render() {
        const {descItem, locked} = this.props;
        var value = descItem.file ? descItem.file : null;

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <Autocomplete
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-file'])}
                    ref='autocomplete'
                    customFilter
                    value={value}
                    items={this.state.fileList}
                    getItemId={(item) => item ? item.id : null}
                    getItemName={(item) => item ? item.name : ''}
                    onSearchChange={this.handleSearchChange}
                    onChange={this.handleChange}
                />
            </div>
        )
    }
};

DescItemFileRef.propsTypes = {
    fundId: React.PropTypes.number.isRequired
};

module.exports = connect(null, null, null, { withRef: true })(DescItemFileRef);