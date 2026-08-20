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
import { AdminLayout } from 'pages/shared/layout/AdminLayout';

const AdminFulltextPage = class AdminFulltextPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return <Ribbon {...this.props} />;
    }

    render() {

        const centerPanel = (
            <div>
                <AdminFulltextReindex {...this.props.fulltext} />
            </div>
        );

        return (
            <AdminLayout
                className="admin-fulltext-page"
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        );
    }
};

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{fulltext: *}}
 */
function mapStateToProps(state) {
    const {adminRegion} = state;

    return {
        fulltext: adminRegion.fulltext,
    };
}

export default connect(mapStateToProps)(AdminFulltextPage);
