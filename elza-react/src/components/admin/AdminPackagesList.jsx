import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux'
import {Table, Button, ButtonToolbar} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components/shared';

import {getPackagesFetchIfNeeded, deletePackage} from 'actions/admin/packages.jsx';
import {downloadFile} from "../../actions/global/download";

/**
 * Komponenta pro zobrazení naimportovaných balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
class AdminPackagesList extends AbstractReactComponent {
    static propTypes = {
        getExportUrl: PropTypes.func.isRequired,
        items: PropTypes.array.isRequired
    };

    componentDidMount() {
        this.props.dispatch(getPackagesFetchIfNeeded());
    }

    handleDeletePackage(code) {
        this.props.dispatch(deletePackage(code));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(getPackagesFetchIfNeeded());
    }

    handleDownload = (code) => {
        const {getExportUrl} = this.props;
        this.props.dispatch(downloadFile(getExportUrl(code)));
    };

    render() {
        const {items} = this.props;

        let mapItems = {};
        items.forEach(item => mapItems[item.code] = item);

        return (
            <Table striped bordered condensed hover>
                <thead>
                <tr>
                    <th>{i18n('admin.packages.label.code')}</th>
                    <th>{i18n('admin.packages.label.name')}</th>
                    <th>{i18n('admin.packages.label.version')}</th>
                    <th>{i18n('admin.packages.label.description')}</th>
                    <th>{i18n('admin.packages.label.dependencies')}</th>
                    <th>{i18n('admin.packages.label.dependencies.by')}</th>
                    <th>{i18n('admin.packages.label.action')}</th>
                </tr>
                </thead>
                <tbody>
                {items.map((item) => <tr key={item.code}>
                    <td>{item.code}</td>
                    <td>{item.name}</td>
                    <td>{item.version}</td>
                    <td>{item.description}</td>
                    <td>{item.dependencies && item.dependencies.map(((dependency, index) => <span title={mapItems[dependency.code].name} key={index}>{dependency.code} ({dependency.version}/{mapItems[dependency.code].version})<br /></span>))}</td>
                    <td>{item.dependenciesBy && item.dependenciesBy.map(((dependency, index) => <span title={mapItems[dependency.code].name} key={index}>{dependency.code}<br /></span>))}</td>
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
}



export default connect()(AdminPackagesList);
