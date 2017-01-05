require ('./ArrDaos.less');

import React from "react";
import {connect} from "react-redux";
import {Icon, Loading, AbstractReactComponent, i18n, ArrDao} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import * as daoActions from "actions/arr/daoActions.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {LazyListBox, ListBox} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';

class ArrDaos extends AbstractReactComponent {

    constructor(props) {
        super(props);

        this.state = {
            selectedItem: null,
        }
    }

    static PropTypes = {
        fund: React.PropTypes.object.isRequired,
        selectedDaoId: React.PropTypes.object,
        nodeId: React.PropTypes.number,
        daoPackageId: React.PropTypes.number,
    };

    refreshRows = () => {
        if (this.refs.listbox) {
            this.refs.listbox.reload();
        }
    };

    componentDidMount() {
        this.handleFetch({}, this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.handleFetch(this.props, nextProps);
    }

    handleFetch = (prevProps, nextProps) => {
        const {fund, nodeId, daoPackageId} = nextProps;

        if (nodeId !== null) {
            this.props.dispatch(daoActions.fetchNodeDaoListIfNeeded(fund.versionId, nodeId));
        } else if (daoPackageId !== null) {
            this.props.dispatch(daoActions.fetchDaoPackageDaoListIfNeeded(fund.versionId, daoPackageId));
        }

        if ((prevProps.fund && prevProps.fund.daoList.isFetching || !prevProps.fund) && nextProps.fund.daoList.fetched && !nextProps.fund.daoList.isFetching) {  // donačetl data, pokusíme se nastavit aktuálně vybranou položku
            const index = indexById(nextProps.fund.daoList.rows, nextProps.selectedDaoId);
            if (index !== null) {
                this.setState({
                    selectedItem: nextProps.fund.daoList.rows[index]
                });
            }
        }
    };

    renderItem = (item) => {
        return <div key={"daos" + item.id} className="item">{item.label}<br /><i><small>{item.code}</small></i></div>
    };

    handleSelect = (item) => {
        this.setState({ selectedItem: item });
    };

    render() {
        const {fund, fund: { daoList }, nodeId, daoPackageId} = this.props;
        const {selectedItem} = this.state;

        if (!daoList.fetched) {
            return <Loading/>;
        }

        let activeIndex;
        if (selectedItem) {
            activeIndex = indexById(daoList.rows, selectedItem.id);
        }

        return (
            <div className="daos-container">
                <div className="daos-list">
                    <div className="title">Digitální entity</div>
                    <div className="daos-list-items">
                        <ListBox
                            activeIndex={activeIndex}
                            className="data-container"
                            items={daoList.rows}
                            renderItemContent={this.renderItem}
                            onFocus={this.handleSelect}
                        />
                    </div>
                </div>
                <div className="daos-detail">
                    {selectedItem && <ArrDao fund={fund} dao={selectedItem} />}
                </div>
            </div>
        );
    }
}

export default connect()(ArrDaos);
