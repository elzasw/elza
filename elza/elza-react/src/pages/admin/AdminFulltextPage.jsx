/**
 * Stránka pro správu fulltextu
 *
 * @author Jiří Vaněk
 * @since 18.1.2016
 */
import React from 'react';

import './AdminFulltextPage.scss';
import {connect} from 'react-redux';
import {AdminFulltextReindex, Ribbon} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';

;

const AdminFulltextPage = class AdminFulltextPage extends React.Component {
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
        const {splitter} = this.props;

        const centerPanel = (
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

export default connect(mapStateToProps)(AdminFulltextPage);
