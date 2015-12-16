/**
 * Stránka rejstříků.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./RegistryPage.less');
var classNames = require('classnames');
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {Ribbon, ModalDialog, NodeTabs, Search, RecordPanel} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {Nav, NavItem} from 'react-bootstrap';
import {recordData} from 'actions/record/recordData'

import {fetchRecordIfNeeded} from 'actions/record/recordList'

var RecordPage = class RecordPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');
        this.buildRibbon = this.buildRibbon.bind(this);
        this.dispatch(fetchRecordIfNeeded());

    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.opened) {
            this.dispatch(fetchRecordIfNeeded());
        }
    }

    buildRibbon() {
        return (
            <Ribbon record {...this.props} />
        )
    }

    handleSelect(record, event) {
        var record = Object.assign({}, record,{selectedId: record.id});
        this.dispatch(recordData(record));


    }

    render() {
        var navRows = this.props.record.items.map(item=>{
            var cls = classNames({
                        active: this.props.record.selectedId === item.id
                        })

            return (
                <div className={cls} onClick={this.handleSelect.bind(this, item)}>
                    <span key={item.id}>{item.record}</span>
                </div>
            )
        });


        var leftPanel = (
            <div>
                <div>
                    <Search />
                </div>
                <div>
                    {(this.props.record.isFetching || !this.props.record.fetched) && <Loading/>}
                    {(!this.props.record.isFetching && this.props.record.fetched) && navRows}
                </div>
            </div>
        )

        var centerPanel = (
            <div>
                <RecordPanel selectedId={this.props.record.selectedId}/>
            </div>
        )

        var rightPanel = (
            <div>
                RIGHT - record
            </div>
        )

        

        return (
            <PageLayout
                className='record-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}

            />
        )
    }
}
function mapStateToProps(state) {
    const {record} = state
    return {
        record
    }
}

module.exports = connect(mapStateToProps)(RecordPage);
