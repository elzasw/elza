/**
 * Stránka pro správu fulltextu
 *
 * @author Jiří Vaněk
 * @since 18.1.2016
 */
import React from 'react';
import ReactDOM from 'react-dom';

require ('./AdminFulltextPage.less');

import {connect} from 'react-redux'
import {Ribbon} from 'components';
import {PageLayout} from 'pages';
import {WebApi} from 'actions';

var AdminFulltextPage = class AdminFulltextPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }

    render() {

        var centerPanel = (
            <div>
                
            </div>
        )

        return (
            <PageLayout
                className='admin-fulltext-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{fulltext: *}}
 */
function mapStateToProps(state) {
    const {fulltext} = state.adminRegion
    return {
        fulltext
    }
}

module.exports = connect(mapStateToProps)(AdminFulltextPage);
