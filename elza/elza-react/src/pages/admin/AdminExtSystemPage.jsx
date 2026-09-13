import React from 'react';

import {connect} from 'react-redux';
import { Icon, RibbonGroup, Utils} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import {AdminExtSystemDetail, AdminExtSystemList, Ribbon, ExtSystemForm} from 'components/index.jsx';
import { AdminLayout } from '../shared/layout/AdminLayout';
import {Shortcuts} from 'react-shortcuts';
import {AREA_EXT_SYSTEM_DETAIL, extSystemCreate, extSystemDelete, extSystemUpdate} from 'actions/admin/extSystem.jsx';
import {Button} from 'components/ui';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {storeFromArea} from 'shared/utils';
import './AdminExtSystemPage.scss';
import AbstractReactComponent from 'components/AbstractReactComponent';

import {PropTypes} from 'prop-types';
import { showConfirmDialog } from 'components/shared/dialog';

var keyModifier = Utils.getKeyModifier();
var defaultKeymap = {
    AdminExtSystemPage: {},
};

// Id jsou převzatá z legacy katalogu beze změny. Tooltipy tlačítek dřív
// ukazovaly klíče ribbon.action.admin.extSystem.*.title, které v katalogu
// nikdy nebyly - vypisovalo se tedy '[klíč]'. Tooltip nese stejný text jako
// popisek akce.
const messages = defineMessages({
    addTitle: { id: 'admin.extSystem.add.title', defaultMessage: 'Vytvoření externího systému' },
    editTitle: { id: 'admin.extSystem.edit.title', defaultMessage: 'Upravení externího systému' },
    deleteConfirm: {
        id: 'admin.extSystem.delete.confirm',
        defaultMessage: 'Opravdu chcete externí systém odstranit?',
    },
    ribbonAdd: { id: 'ribbon.action.admin.extSystem.add', defaultMessage: 'Přidat systém' },
    ribbonEdit: { id: 'ribbon.action.admin.extSystem.edit', defaultMessage: 'Editovat systém' },
    ribbonDelete: { id: 'ribbon.action.admin.extSystem.delete', defaultMessage: 'Smazat systém' },
});

class AdminExtSystemPage extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    /**
     * ADD EXTERNAL SYSTEM
     * *********************************************
     * Uložení nového systému
     */

    handleAddExtSystem = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                this.props.intl.formatMessage(messages.addTitle),
                <ExtSystemForm
                    initialValues={{ multipleLinks: false }}
                    onSubmitForm={data => {
                        return this.props.dispatch(extSystemCreate(data));
                    }}
                />,
            ),
        );
    };

    /**
     * EDIT EXTERNAL SYSTEM
     * *********************************************
     * Upravení systému
     */
    handleEditExtSystem = () => {
        const {data} = this.props.extSystemDetail;
        this.props.dispatch(
            modalDialogShow(
                this,
                this.props.intl.formatMessage(messages.editTitle),
                <ExtSystemForm
                    initialValues={data}
                    onSubmitForm={data => {
                        return this.props.dispatch(extSystemUpdate(data));
                    }}
                />,
            ),
        );
    };

    /**
     * HANDLE DELETE SYSTEM
     * *********************************************
     * Kliknutí na tlačítko pro smazání systému
     */
    handleDeleteExtSystem = async () => {
        const {dispatch} = this.props;
        const response = await dispatch(showConfirmDialog(this.props.intl.formatMessage(messages.deleteConfirm)))
        response
            && this.props.dispatch(extSystemDelete(this.props.extSystemDetail.data.id));
    };

    /**
     * BUILD RIBBON
     * *********************************************
     * Sestavení Ribbon Menu - přidání položek pro osoby
     */
    buildRibbon = () => {
        const {
            extSystemDetail: {id, fetched},
        } = this.props;

        const altActions = [];
        const itemActions = [];

        altActions.push(
            <Button
                key="add-ext-system"
                onClick={this.handleAddExtSystem}
                title={this.props.intl.formatMessage(messages.ribbonAdd)}
                variant={'default'}
            >
                <Icon glyph="fa-plus-circle" />
                <div>
                    <span className="btnText"><FormattedMessage {...messages.ribbonAdd} /></span>
                </div>
            </Button>,
        );
        if (id && fetched) {
            altActions.push(
                <Button
                    key="edit-ext-system"
                    onClick={this.handleEditExtSystem}
                    title={this.props.intl.formatMessage(messages.ribbonEdit)}
                    variant={'default'}
                >
                    <Icon glyph="fa-pencil" />
                    <div>
                        <span className="btnText"><FormattedMessage {...messages.ribbonEdit} /></span>
                    </div>
                </Button>,
            );

            altActions.push(
                <Button
                    key="delete-ext-system"
                    onClick={this.handleDeleteExtSystem}
                    title={this.props.intl.formatMessage(messages.ribbonDelete)}
                    variant={'default'}
                >
                    <Icon glyph="fa-minus-circle" />
                    <div>
                        <span className="btnText"><FormattedMessage {...messages.ribbonDelete} /></span>
                    </div>
                </Button>,
            );
        }

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

        return <Ribbon altSection={altSection} itemSection={itemSection} {...this.props} />;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */
    render() {

        const leftPanel = <AdminExtSystemList />;

        const centerPanel = <AdminExtSystemDetail />;

        return (
            <AdminLayout
                className="admin-ext-system-page"
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        );
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{fulltext: *}}
 */
function mapStateToProps(state) {
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);

    return {
        extSystemDetail,
    };
}

export default connect(mapStateToProps)(injectIntl(AdminExtSystemPage));
