import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, Autocomplete} from 'components/index.jsx';
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {Button} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'

import './DescItemFileRef.less'

class DescItemFileRef extends AbstractReactComponent {

    static PropTypes = {
        fundId: React.PropTypes.number.isRequired,
        onFundFiles: React.PropTypes.func.isRequired,
        onCreateFile:  React.PropTypes.func.isRequired,
    };

    state = {
        fileList: []
    };

    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleChange = (id, valueObj) => {
        this.props.onChange(valueObj);
    };

    handleSearchChange = (text) => {

        text = text == "" ? null : text;

        WebApi.findFundFiles(this.props.fundId, text).then(json => {
            this.setState({
                fileList: json.rows
            })
        })
    };

    handleFundFiles = () => {
        if (this.props.onFundFiles) {
            this.refs.autocomplete.closeMenu();
            this.props.onFundFiles();
        } else {
            console.warn("undefined handleFundFiles");
        }
    };

    handleCreateFile = () => {
        if (this.props.onCreateFile) {
            this.refs.autocomplete.closeMenu();
            this.props.onCreateFile();
        } else {
            console.warn("undefined handleCreateFile");
        }
    };

    renderFooter = () => {
        return <div className="create-file">
            <Button onClick={this.handleCreateFile}><Icon glyph='fa-plus'/>{i18n('arr.fund.files.action.add')}</Button>
            <Button onClick={this.handleFundFiles}>{i18n('arr.panel.title.files')}</Button>
        </div>
    };

    render() {
        const {descItem, locked, readMode} = this.props;
        const value = descItem.file ? descItem.file : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel value={value.name} />;
            } else {
                return <DescItemLabel value="" />;
            }
        }

        const footer = this.renderFooter();

        return <div className='desc-item-value desc-item-value-parts'>
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
                footer={footer}
            />
        </div>;
    }
}

export default connect(null, null, null, { withRef: true })(DescItemFileRef);