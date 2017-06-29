import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, i18n, Loading, Icon} from 'components/shared';
import {AdminExtSystemListItem} from 'components';
import {indexById} from 'stores/app/utils.jsx'
import {extSystemListFetchIfNeeded, extSystemListFilter, extSystemListInvalidate, extSystemDetailFetchIfNeeded, extSystemArrReset, AREA_EXT_SYSTEM_LIST,  AREA_EXT_SYSTEM_DETAIL} from 'actions/admin/extSystem.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';
import {storeFromArea} from 'shared/utils'


import './AdminExtSystemList.less';

/**
 * Komponenta list externích systémů
 */
class AdminExtSystemList extends AbstractReactComponent {

    state = {
        initialized: false
    };

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        this.dispatch(extSystemListFetchIfNeeded());
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.extSystemList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.extSystemList.focus();
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'extSystem', 1) || isFocusFor(focus, 'extSystem', 1, 'list')) {
                this.setState({}, () => {
                    this.refs.extSystemList.focus();
                    focusWasSet()
                })
            }
        }
    };

    handleItemDetail = (item, e) => {
        this.dispatch(extSystemDetailFetchIfNeeded(item.id));
    };

    renderListItem = (item) => {
        return <AdminExtSystemListItem {...item} />
    };

    render() {
        const {extSystemDetail, extSystemList} = this.props;

        let activeIndex = null;
        if (extSystemList.fetched && extSystemDetail.id !== null) {
            activeIndex = indexById(extSystemList.filteredRows, extSystemDetail.id);
        }

        let list;

        const isFetched = !extSystemList.isFetching && extSystemList.fetched;

        if (isFetched) {
            if (extSystemList.rows.length > 0) {
                list = <ListBox
                    ref='extSystemList'
                    items={extSystemList.rows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleItemDetail}
                    onSelect={this.handleItemDetail}
                />;
            } else {
                list = <ul><li className="noResult">{i18n('search.action.noResult')}</li></ul>;
            }
        } else {
            list = <div className="listbox-container"><Loading /></div>;
        }
        // Wrap
        list = <div className="list">{list}</div>;


        return <div className="ext-system-list">
            {list}
        </div>
    }
}

export default connect((state) => {
    const {focus} = state;
    const extSystemList = storeFromArea(state, AREA_EXT_SYSTEM_LIST);
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);
    return {
        focus,
        extSystemList,
        extSystemDetail
    }
})(AdminExtSystemList);
