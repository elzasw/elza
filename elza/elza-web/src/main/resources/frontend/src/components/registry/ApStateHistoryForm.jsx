/**
 * Formulář zobrazení hostorie.
 */
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components/shared';
import {Button, Modal} from 'react-bootstrap';
import {dateToString, getScrollbarWidth, timeToString} from 'components/Utils.jsx'
import ListBox from "../shared/listbox/ListBox";
import {WebApi} from "../../actions";
import {HorizontalLoader} from "../shared";
import "./ApStateHistoryForm.less";

class ApStateHistoryForm extends AbstractReactComponent {

    static propTypes = {
        accessPointId: PropTypes.number.isRequired,
    };

    constructor(props) {
        super(props);
        this.state = {
            fetched: false,
            data: []
        }
    }

    componentDidMount() {
        WebApi.findStateHistories(this.props.accessPointId).then((data) => {
            this.setState({
                fetched: true,
                data: data
            })
        });
    }

    componentWillReceiveProps(nextProps) {
    }

    renderItemContent = (item) => {
        if (item === null) {
            return null;
        }

        return (
            <div className="row-container">
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                {item.description && <div className="col col3" title={item.description}>{item.description}</div>}
                {item.typeText && <div className="col col4" title={item.typeText}>{item.typeText}</div>}
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        )
    };

    getState = (state) => {
        return i18n('ap.history.title.state.' + state);
    };

    renderItem = (data) => {
        const item = data.item;
        return (
            <div className="row-container">
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3">{this.getState(item.state)}</div>
                <div className="col col4" title={item.scope}>{item.scope}</div>
                <div className="col col5" title={item.type}>{item.type}</div>
                <div className="col col6" title={item.comment}>{item.comment}</div>
                <div className="col col7">{item.username ? item.username : <i>System</i>}</div>
            </div>
        )
    };

    render() {
        const {onClose} = this.props;
        const {data, fetched} = this.state;

        let content = <ListBox items={data} renderItemContent={this.renderItem}/>;

        return (
            <div className="ap-state-history-form-container">
                <Modal.Body>
                    <div className="changes-listbox-container">
                        <div className="header-container">
                            <div className="col col1">{i18n("ap.history.title.change.date")}</div>
                            <div className="col col2">{i18n("ap.history.title.change.time")}</div>
                            <div className="col col3">{i18n("ap.history.title.change.state")}</div>
                            <div className="col col4">{i18n("ap.history.title.change.scope")}</div>
                            <div className="col col5">{i18n("ap.history.title.change.type")}</div>
                            <div className="col col6">{i18n("ap.history.title.change.comment")}</div>
                            <div className="col col7">{i18n("ap.history.title.change.user")}</div>
                            {/*<div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>*/}
                        </div>
                        {fetched ? content : <HorizontalLoader/>}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect()(ApStateHistoryForm);
