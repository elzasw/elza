/**
 * Komponenta zobrazení požadavků ve frontě.
 */
import React from 'react';
import {connect} from 'react-redux';
import {Table} from 'react-bootstrap';
import {Button} from '../../components/ui';
import {AbstractReactComponent, StoreHorizontalLoader} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang';
import { messageFor } from 'components/shared/lang/dynamicMessage';

// Id jsou převzatá z legacy katalogu beze změny; opraven jen překlep
// "Poslední pokud o odeslání" -> "pokus".
const messages = defineMessages({
    deleteConfirm: { id: 'requestQueue.delete.confirm', defaultMessage: 'Opravdu chcete zmazat položku z fronty?' },
    create: { id: 'requestQueue.title.create', defaultMessage: 'Vytvořeno' },
    attemptToSend: { id: 'requestQueue.title.attemptToSend', defaultMessage: 'Poslední pokus o odeslání' },
    description: { id: 'requestQueue.title.description', defaultMessage: 'Popis' },
    error: { id: 'requestQueue.title.error', defaultMessage: 'Chyba odeslání' },
    username: { id: 'requestQueue.title.username', defaultMessage: 'Uživatel' },
});

// Typy požadavků. Klíč se skládal za běhu, což statický extraktor nevidí;
// množina je uzavřená (viz konstanty DIGITIZATION/DAO/DAO_LINK).
const requestTypeMessages = defineMessages({
    DIGITIZATION: { id: 'arr.request.title.type.DIGITIZATION', defaultMessage: 'Požadavek na digitalizaci' },
    DAO: { id: 'arr.request.title.type.DAO', defaultMessage: 'Požadavek na skartaci/delimitaci' },
    DAO_LINK: {
        id: 'arr.request.title.type.DAO_LINK',
        defaultMessage: 'Požadavek na připojení k/odpojení od JP',
    },
});

const daoRequestTypeMessages = defineMessages({
    DESTRUCTION: { id: 'arr.request.title.type.dao.DESTRUCTION', defaultMessage: 'Požadavek na skartaci' },
    TRANSFER: { id: 'arr.request.title.type.dao.TRANSFER', defaultMessage: 'Požadavek na delimitaci' },
});
import {Ribbon} from 'components/index.jsx';
import { AdminLayout } from '../shared/layout/AdminLayout';
import './AdminRequestsQueuePage.scss';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import {
    createDaoLinkName,
    createDigitizationName,
    DAO,
    DAO_LINK,
    DIGITIZATION,
    getRequestType,
} from 'components/arr/ArrUtils.jsx';
import {dateTimeToString} from 'components/Utils.jsx';
import {WebApi} from 'actions/index.jsx';
import { showConfirmDialog } from 'components/shared/dialog';

class AdminRequestsQueuePage extends AbstractReactComponent {
    buildRibbon() {
        return <Ribbon {...this.props} />;
    }

    componentDidMount() {
        this.props.dispatch(arrRequestActions.fetchInQueueListIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(arrRequestActions.fetchInQueueListIfNeeded());
    }

    handleDelete = async (item) => {
        const {dispatch} = this.props;
        const response = await dispatch(showConfirmDialog(this.props.intl.formatMessage(messages.deleteConfirm)))
        if (response) {
            WebApi.removeArrRequestQueueItem(item.request.id);
        }
    };

    createDescription = (type, request) => {
        const {userDetail} = this.props;
        switch (type) {
            case DIGITIZATION: {
                return ' - ' + createDigitizationName(request, userDetail);
            }
            case DAO_LINK: {
                return ' - ' + createDaoLinkName(request, userDetail);
            }
            case DAO: {
                return ' - ' + this.props.intl.formatMessage(messageFor(daoRequestTypeMessages, request.type, daoRequestTypeMessages.DESTRUCTION));
            }
            default:
                return 'Unknown type: ' + type;
        }
    };

    render() {
        const {requestInQueueList} = this.props;

        let centerPanel = (
            <div>
                <StoreHorizontalLoader store={requestInQueueList} />
                {requestInQueueList.fetched && (
                    <Table striped bordered condensed hover>
                        <thead>
                            <tr>
                                <th><FormattedMessage {...messages.create} /></th>
                                <th><FormattedMessage {...messages.attemptToSend} /></th>
                                <th><FormattedMessage {...messages.description} /></th>
                                <th><FormattedMessage {...messages.error} /></th>
                                <th><FormattedMessage {...messages.username} /></th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {requestInQueueList.rows.map(item => {
                                let type = getRequestType(item.request);
                                return (
                                    <tr key={item.id}>
                                        <td>{dateTimeToString(new Date(item.create))}</td>
                                        <td>{item.attemptToSend && dateTimeToString(new Date(item.attemptToSend))}</td>
                                        <td>
                                            <FormattedMessage {...messageFor(requestTypeMessages, type, requestTypeMessages.DAO)} />{' '}
                                            {this.createDescription(type, item.request)}
                                        </td>
                                        <td>{item.error}</td>
                                        <td>{item.request.username}</td>
                                        <td>
                                            <Button onClick={() => this.handleDelete(item)}>
                                                <FormattedMessage {...globalMessages.delete} />
                                            </Button>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </Table>
                )}
            </div>
        );

        return (
            <AdminLayout
                className="admin-requestsQueue-page"
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        );
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {
        app: {requestInQueueList},
        userDetail,
    } = state;
    return {
        requestInQueueList,
        userDetail,
    };
}

export default connect(mapStateToProps)(injectIntl(AdminRequestsQueuePage));
