/**
 * Komponenta pro zobrazení naimportovaných balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import {connect} from 'react-redux'
import {Table, Button, ButtonToolbar} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components';

import {getPackagesFetchIfNeeded, deletePackage} from 'actions/admin/packages';

var AdminPackagesList = class AdminPackagesList extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.dispatch(getPackagesFetchIfNeeded());
    }

    handleDeletePackage(code) {
        this.dispatch(deletePackage(code));
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(getPackagesFetchIfNeeded());
    }

    render() {
        const items = this.props.items.map((item) => {
            return (
                    <tr key={item.code}>
                        <td>{item.code}</td>
                        <td>{item.name}</td>
                        <td>{item.version}</td>
                        <td>{item.description}</td>
                        <td>
                            <ButtonToolbar>
                                <Button href={this.props.getExportUrl(item.code)} bsSize="xsmall">Stáhnout</Button>
                                <Button onClick={this.handleDeletePackage.bind(this, item.code)} bsSize="xsmall">Smazat</Button>
                            </ButtonToolbar>
                        </td>
                    </tr>
            );
        });


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
                {items}
                </tbody>
            </Table>
        );
    }
};

AdminPackagesList.propTypes = {
    getExportUrl: React.PropTypes.func.isRequired,
    items: React.PropTypes.array.isRequired
};

module.exports = connect()(AdminPackagesList);
