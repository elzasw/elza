/**
 * Komponenta pro import balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../ui';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';

import {importPackage} from 'actions/admin/packages.jsx';

class AdminPackagesUpload extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {
            disabled: true,
        };
        this.fileInput = React.createRef();
    }

    handleUpload = () => {
        let data = new FormData();
        data.append('file', this.fileInput.current.files[0]);
        this.props.dispatch(importPackage(data));
    };

    handleChangeFile = () => {
        this.setState({disabled: this.fileInput.current.files.length === 0});
    };

    render() {
        return (
            <div>
                <FormInput onChange={this.handleChangeFile} ref={this.fileInput} name="file" type="file" />
                <Button variant="outline-secondary" disabled={this.state.disabled} onClick={this.handleUpload}>
                    {i18n('admin.packages.action.import')}
                </Button>
            </div>
        );
    }
}

export default connect()(AdminPackagesUpload);
