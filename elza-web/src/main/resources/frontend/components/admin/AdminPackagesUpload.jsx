/**
 * Komponenta pro import balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';

import {importPackage} from 'actions/admin/packages.jsx';

var AdminPackagesUpload = class AdminPackagesUpload extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            disabled: true
        };

        this.handleUpload = this.handleUpload.bind(this);
        this.handleChangeFile = this.handleChangeFile.bind(this);
    }

    handleUpload() {
        var file = this.refs.file;
        var data = new FormData();
        data.append("file", ReactDOM.findDOMNode(file.refs.input).files[0]);
        this.dispatch(importPackage(data));
    }

    handleChangeFile() {
        console.log(ReactDOM.findDOMNode(this.refs.file.refs.input));
        this.setState({disabled: ReactDOM.findDOMNode(this.refs.file.refs.input).files.length == 0});
    }

    render() {

        return (
                <div>
                    <FormInput onChange={this.handleChangeFile} ref="file" name="file" type="file" />
                    <Button disabled={this.state.disabled} onClick={this.handleUpload}>{i18n('admin.packages.action.import')}</Button>
                </div>
        );
    }
}

module.exports = connect()(AdminPackagesUpload);
