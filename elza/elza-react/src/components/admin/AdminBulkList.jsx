import React from "react";
import {
    AbstractReactComponent,
    CollapsablePanel
} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import AdminBulkHeader from "./AdminBulkHeader";
import AdminBulkBody from "./AdminBulkBody";

import './AdminBulkList.scss';
import { FormattedMessage, defineMessages } from "react-intl";
import { messageFor } from "components/shared/lang/dynamicMessage";

// Klíč se skládal z typu fronty, což statický extraktor nevidí. Množina typů je
// uzavřená (odpovídá typům front na serveru), takže stačí ji vypsat a vybírat.
const queueTypeMessages = defineMessages({
    NODE: { id: 'admin.bulk.header.title.NODE', defaultMessage: 'Validace JP' },
    BULK: { id: 'admin.bulk.header.title.BULK', defaultMessage: 'Hromadné akce' },
    OUTPUT: { id: 'admin.bulk.header.title.OUTPUT', defaultMessage: 'Výstupy' },
    AP: { id: 'admin.bulk.header.title.AP', defaultMessage: 'Validace AE' },
    EXPORT: { id: 'admin.bulk.header.title.EXPORT', defaultMessage: 'Publikace' },
    AIP: { id: 'admin.bulk.header.title.AIP', defaultMessage: 'Archivní balíčky' },
    BATCH_IMPORT: { id: 'admin.bulk.header.title.BATCH_IMPORT', defaultMessage: 'Dávkový import' },
});
import Loading from "../shared/loading/Loading";

class AdminBulkList extends AbstractReactComponent {
    state = {
        fetched: false,
        asyncRequest: []
    };

    componentDidMount() {
        this.refresh();
    }

    componentWillUnmount() {
        clearTimeout(this.timer);
    }

    refresh = () => {
        WebApi.getAsyncRequestInfo().then(newData => {
            this.setState({fetched: true, asyncRequest: newData});
            this.timer = setTimeout(this.refresh, 10000);
        });
    }

    render() {
        let {fetched, asyncRequest} = this.state;

        if (!fetched) {
            return <Loading />;
        }

        return <div>
            {asyncRequest.map((request, index) => {
                const type = request.type;
                return <CollapsablePanel tabIndex={index} eventKey={index} header={<AdminBulkHeader name={<FormattedMessage {...messageFor(queueTypeMessages, type, queueTypeMessages.BULK)} />} data={request} />}>
                    <AdminBulkBody type={type}/>
                </CollapsablePanel>
            })}
        </div>
    }
}

export default AdminBulkList;
