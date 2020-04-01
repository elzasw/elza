import React from 'react';
import {RibbonGroup, Utils} from 'components/shared';
import {AdminLogsDetail, Ribbon} from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import {Shortcuts} from 'react-shortcuts';
import './AdminExtSystemPage.scss';
import AbstractReactComponent from '../../components/AbstractReactComponent';

import {PropTypes} from 'prop-types';

var keyModifier = Utils.getKeyModifier();
var defaultKeymap = {
    AdminLogsPage: {},
};

class AdminLogsPage extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};
    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }
    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }
    handleShortcuts = () => {};

    /**
     * BUILD RIBBON
     * *********************************************
     * Sestavení Ribbon Menu - přidání položek pro osoby
     */
    buildRibbon = () => {
        const altActions = [];
        const itemActions = [];

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt-actions" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item-actions" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon admin altSection={altSection} itemSection={itemSection} {...this.props} />;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */
    render() {
        const leftPanel = null;

        const centerPanel = <AdminLogsDetail />;

        return (
            <Shortcuts name="AdminLogsPage" handler={this.handleShortcuts}>
                <PageLayout
                    splitter={{}}
                    className="admin-logs-page"
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                />
            </Shortcuts>
        );
    }
}

export default AdminLogsPage;
