import React from 'react';

import {connect} from 'react-redux';
import {i18n, Icon, RibbonGroup, Utils} from 'components/shared';
import {AdminExtSystemDetail, AdminExtSystemList, Ribbon} from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import {Shortcuts} from 'react-shortcuts';
import {AREA_EXT_SYSTEM_DETAIL, extSystemCreate, extSystemDelete, extSystemUpdate} from 'actions/admin/extSystem.jsx';
import {Button} from '../../components/ui';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import ExtSystemForm from 'components/admin/ExtSystemForm.jsx';
import {storeFromArea} from 'shared/utils';
import './AdminExtSystemPage.scss';
import AbstractReactComponent from '../../components/AbstractReactComponent';

import {PropTypes} from 'prop-types';

var keyModifier = Utils.getKeyModifier();
var defaultKeymap = {
    AdminExtSystemPage: {},
};

class AdminExtSystemPage extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    handleShortcuts = () => {
    };

    /**
     * ADD EXTERNAL SYSTEM
     * *********************************************
     * Uložení nového systému
     */

    handleAddExtSystem = () => {
        this.props.dispatch(modalDialogShow(this, i18n('admin.extSystem.add.title'), <ExtSystemForm
            onSubmitForm={(data) => {
                return this.props.dispatch(extSystemCreate(data));
            }}/>));
    };

    /**
     * ADD EXTERNAL SYSTEM
     * *********************************************
     * Upravení systému
     */
    handleEditExtSystem = () => {
        const {data} = this.props.extSystemDetail;
        this.props.dispatch(modalDialogShow(this, i18n('admin.extSystem.edit.title'), <ExtSystemForm
            initialValues={data} onSubmitForm={(data) => {
            return this.props.dispatch(extSystemUpdate(data));
        }}/>));
    };

    /**
     * HANDLE DELETE SYSTEM
     * *********************************************
     * Kliknutí na tlačítko pro smazání systému
     */
    handleDeleteExtSystem = () => {
        window.confirm(i18n('admin.extSystem.delete.confirm')) && this.props.dispatch(extSystemDelete(this.props.extSystemDetail.data.id));
    };

    /**
     * BUILD RIBBON
     * *********************************************
     * Sestavení Ribbon Menu - přidání položek pro osoby
     */
    buildRibbon = () => {
        const {extSystemDetail: {id, fetched}} = this.props;

        const altActions = [];
        const itemActions = [];

        altActions.push(
            <Button key="add-ext-system" onClick={this.handleAddExtSystem}
                    title={i18n('ribbon.action.admin.extSystem.add.title')} variant={'default'}>
                <Icon glyph="fa-download"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.extSystem.add')}</span></div>
            </Button>,
        );
        if (id && fetched) {
            itemActions.push(
                <Button key="edit-ext-system" onClick={this.handleEditExtSystem}
                        title={i18n('ribbon.action.admin.extSystem.edit.title')} variant={'default'}>
                    <Icon glyph="fa-download"/>
                    <div><span className="btnText">{i18n('ribbon.action.admin.extSystem.edit')}</span></div>
                </Button>,
            );

            itemActions.push(
                <Button key="delete-ext-system" onClick={this.handleDeleteExtSystem}
                        title={i18n('ribbon.action.admin.extSystem.delete.title')} variant={'default'}>
                    <Icon glyph="fa-minus-circle"/>
                    <div><span className="btnText">{i18n('ribbon.action.admin.extSystem.delete')}</span></div>
                </Button>,
            );
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key='alt-actions' className="small">{altActions}</RibbonGroup>;
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key='item-actions' className="small">{itemActions}</RibbonGroup>;
        }

        return <Ribbon admin altSection={altSection} itemSection={itemSection} {...this.props} />;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */
    render() {
        const {splitter} = this.props;

        const leftPanel = <AdminExtSystemList/>;

        const centerPanel = <AdminExtSystemDetail/>;

        return <Shortcuts name='AdminExtSystemPage' handler={this.handleShortcuts}>
            <PageLayout
                splitter={splitter}
                className='admin-ext-system-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        </Shortcuts>;
    }
}


/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{fulltext: *}}
 */
function mapStateToProps(state) {
    const {splitter} = state;
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);

    return {
        splitter,
        extSystemDetail,
    };
}

export default connect(mapStateToProps)(AdminExtSystemPage);
