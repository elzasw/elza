import React from 'react';
import {connect} from 'react-redux'
import {Table, Button, ButtonToolbar} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components/index.jsx';

import {getPackagesFetchIfNeeded, deletePackage} from 'actions/admin/packages.jsx';
import {downloadFile} from "../../actions/global/download";

/**
 * Komponenta pro zobrazení naimportovaných balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
class AdminPackagesList extends AbstractReactComponent {
    static PropTypes = {
        getExportUrl: React.PropTypes.func.isRequired,
        items: React.PropTypes.array.isRequired
    };

    componentDidMount() {
        this.dispatch(getPackagesFetchIfNeeded());
    }

    handleDeletePackage(code) {
        this.dispatch(deletePackage(code));
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getPackagesFetchIfNeeded());
    }

    handleDownload = (code) => {
        const {getExportUrl} = this.props;
        this.dispatch(downloadFile("package-" + code, getExportUrl(code)));
    };

    render() {
        const {items} = this.props;

        return (
            <Table striped bordered condensed hover>
                <thead>
                <tr>
                    <th>{i18n('admin.packages.label.code')}</th>
                    <th>{i18n('admin.packages.label.name')}</th>
                    <th>{i18n('admin.packages.label.version')}</th>
                    <th>{i18n('admin.packages.label.description')}</th>
                    <th>{i18n('admin.packages.label.action')}</th>
                </tr>
                </thead>
                <tbody>
                {items.map((item) => <tr key={item.code}>
                    <td>{item.code}</td>
                    <td>{item.name}</td>
                    <td>{item.version}</td>
                    <td>{item.description}</td>
                    <td>
                        <ButtonToolbar>
                            <Button onClick={() => this.handleDownload(item.code)} bsSize="xsmall">{i18n("global.action.download")}</Button>
                            <Button onClick={this.handleDeletePackage.bind(this, item.code)} bsSize="xsmall">{i18n("global.action.delete")}</Button>
                        </ButtonToolbar>
                    </td>
                </tr>)}
                </tbody>
            </Table>
        );
    }
};



export default connect()(AdminPackagesList);
