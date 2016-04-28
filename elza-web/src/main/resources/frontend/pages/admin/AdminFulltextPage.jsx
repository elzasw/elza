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
import {Ribbon, AdminFulltextReindex} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';

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
console.log(1111111, this.props);
        const {splitter} = this.props;

        var centerPanel = (
            <div>
                <AdminFulltextReindex {...this.props.fulltext} />
            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
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
    const {splitter, adminRegion} = state
    
    return {
        splitter,
        fulltext: adminRegion.fulltext
    }
}

module.exports = connect(mapStateToProps)(AdminFulltextPage);
