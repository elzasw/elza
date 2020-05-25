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
    constructor(props) {
        super(props);
        this.stop = false;
        this.state = {
            fetched: false,
            asyncRequest: []
        };
    }

    componentDidMount() {
        this.refresh();
    }

    componentWillUnmount() {
        this.stop = true;
    }

    refresh = () => {
        if (this.stop) {
            return;
        }

        WebApi.getAsyncRequestInfo().then(
            newData => {
                this.setState({
                    ...this.state,
                    fetched: true,
                    asyncRequest: newData
                }, () => {
                    setTimeout(this.refresh, 10000);
                });
            }
        )
    }

    render() {
        let {fetched, asyncRequest} = this.state;

        if (!fetched) {
            return <Loading />;
        }

        return <div>
            {asyncRequest.map((request, index) => {
                const type = request.type;
                return <CollapsablePanel tabIndex={index} header={<AdminBulkHeader name={i18n('admin.bulk.header.title.' + type)} data={request} />}>
                    <AdminBulkBody type={type}/>
                </CollapsablePanel>
            })}
        </div>
    }
}

export default AdminBulkList;
