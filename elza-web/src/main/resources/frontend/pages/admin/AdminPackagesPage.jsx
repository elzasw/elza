import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {Ribbon, AdminPackagesList, AdminPackagesUpload, i18n, Icon, RibbonGroup} from 'components';
import {PageLayout} from 'pages/index.jsx';
import {UrlFactory} from 'actions/index.jsx';
import {Button} from 'react-bootstrap'

import './AdminPackagesPage.less';

/**
 * Stránka pro správu importovaných balíčků
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
class AdminPackagesPage extends React.Component {

    handleImportPackage = () => {
        if(confirm(i18n('global.title.processAction'))) {
            console.log('importPackage');
        }
    };
    handleExportPackage = () => {
        if(confirm(i18n('global.title.processAction'))) {
            console.log('exportPackage');
        }
    };
    handleDeletePackage = () => {
        if(confirm(i18n('global.title.processAction'))) {
            console.log('deletePackage');
        }
    };

    buildRibbon = () => {
        const altActions = [];
        const itemActions = [];

        altActions.push(
            <Button key="import-package" onClick={this.handleImportPackage} title={i18n('ribbon.action.admin.package.import.title')}>
                <Icon glyph="fa-download"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.package.import')}</span></div>
            </Button>
        );
        itemActions.push(
            <Button key="export-package" onClick={this.handleExportPackage} title={i18n('ribbon.action.admin.package.export.title')}>
                <Icon glyph="fa-upload"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.package.export')}</span></div>
            </Button>
        );

        itemActions.push(
            <Button key="delete-package" onClick={this.handleDeletePackage} title={i18n('ribbon.action.admin.package.delete.title')}>
                <Icon glyph="fa-minus-circle"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.package.delete')}</span></div>
            </Button>
        );

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key='alt-actions' className="small">{altActions}</RibbonGroup>
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key='item-actions' className="small">{itemActions}</RibbonGroup>
        }
        return <Ribbon admin altSection={altSection} itemSection={itemSection} {...this.props} />;
    };

    render() {
        const {splitter} = this.props;

        const centerPanel = <div>
            <AdminPackagesList getExportUrl={UrlFactory.exportPackage} {...this.props.packages} />
            <AdminPackagesUpload />
        </div>;

        return (
            <PageLayout
                splitter={splitter}
                className='admin-packages-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {splitter, adminRegion: {packages}} = state;
    return {
        splitter,
        packages
    }
}

export default connect(mapStateToProps)(AdminPackagesPage);
