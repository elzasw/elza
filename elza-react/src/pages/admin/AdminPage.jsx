/**
 * Úvodní stránka administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import {connect} from 'react-redux';
import {Icon, RibbonGroup} from 'components/shared';
import {FormattedMessage, defineMessages, injectIntl} from 'react-intl';
import {Button} from '../../components/ui';
import {developerSet} from 'actions/global/developer.jsx';
import {resetLocalStorage} from 'actions/store/storeEx.jsx';
import {WebApi} from 'actions/index.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';

import './AdminPage.scss';
import AbstractReactComponent from '../../components/AbstractReactComponent';
import Ribbon from '../../components/page/Ribbon';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { showConfirmDialog } from 'components/shared/dialog';
import { StatsAdmin } from 'components/shared/stats';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    reindexing: {
        id: 'admin.fulltext.message.reindexing',
        defaultMessage: 'Probíhá reindexace...',
    },
    processAction: {
        id: 'global.title.processAction',
        defaultMessage: 'Opravdu chcete provést vybranou akci?',
    },
    resetServerCacheSuccess: {
        id: 'admin.resetServerCache.success',
        defaultMessage: 'Serverové cache byly resetovány',
    },
    developer: {
        id: 'ribbon.action.admin.developer',
        defaultMessage: 'Developer mode',
    },
    reindex: {
        id: 'ribbon.action.admin.reindex',
        defaultMessage: 'Přepočítat indexy',
    },
    reindexTitle: {
        id: 'ribbon.action.admin.reindex.title',
        defaultMessage: 'Přepočítat indexy',
    },
    resetLocalStorage: {
        id: 'ribbon.action.admin.resetLocalStorage',
        defaultMessage: 'Smazat lokální cache',
    },
    resetLocalStorageTitle: {
        id: 'ribbon.action.admin.resetLocalStorage.title',
        defaultMessage:
            'Smaže historii posledně otevřených položek a uvede zobrazení aplikace do výchozího stavu',
    },
    resetServerCache: {
        id: 'ribbon.action.admin.resetServerCache',
        defaultMessage: 'Smazat serverovou cache',
    },
    resetServerCacheTitle: {
        id: 'ribbon.action.admin.resetServerCache.title',
        defaultMessage: 'Smaže všechny cache na serveru',
    },
});

class AdminPage extends AbstractReactComponent {
    UNSAFE_componentWillReceiveProps = nextProps => {
        this.fetchData(nextProps);
    };

    componentDidMount = () => {
        this.fetchData(this.props);
    };

    fetchData = props => {
        const {fetched, userDetail} = props;

        if (userDetail.hasOne(perms.ADMIN)) {
            if (!fetched) {
                props.dispatch(getIndexStateFetchIfNeeded());
            }
        }
    };

    renderReindexing = () => {
        return (
            <div>
                <FormattedMessage {...messages.reindexing} />
            </div>
        );
    };

    startReindexing = () => {
        this.props.dispatch(reindex());
    };

    handleDeveloperMode = () => {
        this.props.dispatch(developerSet(!this.props.developer.enabled));
    };

    handleResetLocalStorage = async () => {
        const {dispatch} = this.props;
        const response = await dispatch(
            showConfirmDialog(this.props.intl.formatMessage(messages.processAction)),
        );
        if (response) {
            resetLocalStorage();
        }
    };

    handleResetServerCache = async () => {
        const {dispatch} = this.props;
        const response = await dispatch(
            showConfirmDialog(this.props.intl.formatMessage(messages.processAction)),
        );
        if (response) {
            WebApi.resetServerCache().then(() => {
                this.props.dispatch(
                    addToastrSuccess(this.props.intl.formatMessage(messages.resetServerCacheSuccess)),
                );
            });
        }
    };

    buildRibbon() {
        const {
            userDetail,
            fulltext: {indexing},
        } = this.props;

        const altActions = [];

        if (userDetail.hasOne(perms.FUND_ARR_ALL, perms.FUND_ARR, perms.FUND_RD_ALL, perms.FUND_RD)) {
            altActions.push(
                <Button
                    active={this.props.developer.enabled}
                    key="developerMode"
                    onClick={this.handleDeveloperMode}
                    variant={'default'}
                >
                    <Icon glyph="fa-cogs" />
                    <div>
                        <span className="btnText">
                            <FormattedMessage {...messages.developer} />
                        </span>
                    </div>
                </Button>,
            );
        }

        if (userDetail.hasOne(perms.ADMIN)) {
            altActions.push(
                <Button
                    key="reindex"
                    onClick={this.startReindexing}
                    disabled={indexing}
                    title={this.props.intl.formatMessage(messages.reindexTitle)}
                    variant={'default'}
                >
                    <Icon glyph="fa-search" />
                    <div>
                        <span className="btnText">
                            <FormattedMessage {...(indexing ? messages.reindexing : messages.reindex)} />
                        </span>
                    </div>
                </Button>,
            );
            altActions.push(
                <Button
                    key="resetLocalStorage"
                    onClick={this.handleResetLocalStorage}
                    title={this.props.intl.formatMessage(messages.resetLocalStorageTitle)}
                    variant={'default'}
                >
                    <Icon glyph="fa-times" />
                    <div>
                        <span className="btnText">
                            <FormattedMessage {...messages.resetLocalStorage} />
                        </span>
                    </div>
                </Button>,
            );
            altActions.push(
                <Button
                    key="resetServerCache"
                    onClick={this.handleResetServerCache}
                    title={this.props.intl.formatMessage(messages.resetServerCacheTitle)}
                    variant={'default'}
                >
                    <Icon glyph="fa-times" />
                    <div>
                        <span className="btnText">
                            <FormattedMessage {...messages.resetServerCache} />
                        </span>
                    </div>
                </Button>,
            );
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        return <Ribbon altSection={altSection} {...this.props} />;
    }

    render() {

        return (
            <AdminLayout
                className="admin-packages-page"
                ribbon={this.buildRibbon()}
                centerPanel={<StatsAdmin />}
            />
        );
    }
}

function mapStateToProps(state) {
    const {
        developer,
        userDetail,
        adminRegion: {fulltext},
    } = state;

    return {
        developer,
        userDetail,
        fulltext,
    };
}

export default connect(mapStateToProps)(injectIntl(AdminPage));
