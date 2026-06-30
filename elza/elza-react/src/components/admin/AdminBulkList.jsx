import React from "react";
import {
    AbstractReactComponent,
    CollapsablePanel
} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import AdminBulkHeader from "./AdminBulkHeader";
import AdminBulkBody from "./AdminBulkBody";

import './AdminBulkList.scss';
import i18n from "../i18n";
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
                return <CollapsablePanel tabIndex={index} eventKey={index} header={<AdminBulkHeader name={i18n('admin.bulk.header.title.' + type)} data={request} />}>
                    <AdminBulkBody type={type}/>
                </CollapsablePanel>
            })}
        </div>
    }
}

export default AdminBulkList;
