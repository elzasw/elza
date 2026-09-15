/**
 * Komponenta pro import balíčků.
 */
import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent, FormInput} from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    import: { id: 'admin.packages.action.import', defaultMessage: 'Importovat' },
});
import {
    Button as FluentButton,
    tokens,
} from "@fluentui/react-components";

import { importPackage } from 'actions/admin/packages.jsx';

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
        this.setState({ disabled: this.fileInput.current.files.length === 0 });
    };

    render() {

        return (
            <div style={{ padding: tokens.spacingHorizontalM, display: "flex", columnGap: tokens.spacingHorizontalS }} >
                <FormInput onChange={this.handleChangeFile} ref={this.fileInput} name="file" type="file" />
                <FluentButton disabled={this.state.disabled} onClick={this.handleUpload}>
                    <FormattedMessage {...messages.import} />
                </FluentButton>
            </div>
        );
    }
}

export default connect()(AdminPackagesUpload);
