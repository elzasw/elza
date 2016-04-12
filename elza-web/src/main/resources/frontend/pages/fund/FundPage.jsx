/**
 * Stránka archivní soubory.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent, ImportForm} from 'components';
import {NodeTabs, FundTreeTabs} from 'components';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow} from 'actions/global/modalDialog'
import {createFund} from 'actions/arr/fund'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions'
import {setInputFocus, dateToString} from 'components/Utils'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var FundPage = class FundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleAddFund', 'handleImport')

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
    }

    handleAddFund() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'),
            <FundForm create onSubmitForm={(data) => {this.dispatch(createFund(data))}}/>));
    }

    handleImport() {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.fund'),
                <ImportForm fund={true}/>
            )
        );
    }

    buildRibbon() {
        var altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div></Button>
        )
        altActions.push(
            <Button key="fa-import" onClick={this.handleImport}><Icon glyph='fa-download'/>
                <div><span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span></div>
            </Button>
        )

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }

        return (
            <Ribbon ref='ribbon' fund altSection={altSection} {...this.props} />
        )
    }

    render() {
        const {splitter} = this.props;

        var centerPanel = (
            <div className='splitter-home'>
xxx
            </div>
        )

centerPanel =
    <div>
        {centerPanel}
    </div>

        return (
            <PageLayout
                splitter={splitter}
                className='fund-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const {focus, splitter} = state
    return {
        focus,
        splitter,
    }
}

module.exports = connect(mapStateToProps)(FundPage);

