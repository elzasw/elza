import React from "react";
import {WebApi} from 'actions/index.jsx';
import './AdminBulkBody.scss';
import i18n from "../i18n";

class AdminBulkBody extends React.Component {
    constructor(props) {
        super(props);
        this.stop = false;
        this.state = {
            fetched: false,
            asyncRequestDetail: []
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

        WebApi.getAsyncRequestDetail(this.props.type).then(
            newData => {
                this.setState({
                    ...this.state,
                    fetched: true,
                    asyncRequestDetail: newData
                }, () => {
                    setTimeout(this.refresh, 10000);
                });
            }
        )
    }

    render() {
        return <div>
            {
                (this.state.asyncRequestDetail.length > 0) ?
                    this.state.asyncRequestDetail.map((row) => (
                         <div className={"bulk-row"}>
                             {row.fund &&
                                 <div className={"bulk-detail"}>
                                     <b title={row.fund.name + " (id: " + row.fund.id + ")"}>{row.fund.name}: </b>
                                     {row.requestCount}x
                                 </div>
                             }
                        </div>
                    )) :
                    <div className={"bulk-detail"}>
                        <b>{i18n('admin.bulk.detail.queue.empty')}</b>
                    </div>
            }
        </div>
    }
}


export default AdminBulkBody;
