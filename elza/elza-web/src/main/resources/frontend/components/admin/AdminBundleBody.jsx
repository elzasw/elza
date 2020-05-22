import React from "react";
import {WebApi} from 'actions/index.jsx';
import './AdminBundleBody.less';

class AdminBundleBody extends React.Component {
    constructor(props) {
        super(props);
        this.stop = false;
        this.state = {
            fetched: false,
            asyncRequestDetail: []
        };
    }

    componentDidMount() {
        WebApi.getAsyncRequestInfo(this.props.type).then(
            data => {
                this.setState({asyncRequestDetail: data, fetched: true}, () => {
                        this.refresh();
                });
            }
        )
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
                         <div className={"bundle-row"}>
                             {row.fund &&
                                 <div className={"bundle-detail"}>
                                     <b title={row.fund.name + " (id: " + row.fund.id + ")"}>{row.fund.name}: </b>
                                     {row.requestCount}x
                                 </div>
                             }
                        </div>
                    )) :
                    <div className={"bundle-detail"}>
                        <b>Nebyly nalezeny žádné akce</b>
                    </div>
            }
        </div>
    }
}


export default AdminBundleBody;
