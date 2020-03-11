/**
 * Komponenta list externích systémů
 */
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, ListBox, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx';
import {
    AREA_EXT_SYSTEM_DETAIL,
    AREA_EXT_SYSTEM_LIST,
    extSystemDetailFetchIfNeeded,
    extSystemListFetchIfNeeded,
} from 'actions/admin/extSystem.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx';
import {storeFromArea} from 'shared/utils';


import './AdminExtSystemList.scss';
import AdminExtSystemListItem from './AdminExtSystemListItem';
import {FOCUS_KEYS} from '../../constants.tsx';

class AdminExtSystemList extends AbstractReactComponent {

    state = {
        initialized: false,
    };

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        this.props.dispatch(extSystemListFetchIfNeeded());
    };

    trySetFocus = (props = this.props) => {
        const { focus } = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.extSystemList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.extSystemList.focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.ADMIN_EXT_SYSTEM, 1) || isFocusFor(focus, FOCUS_KEYS.ADMIN_EXT_SYSTEM, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.extSystemList.focus();
                    focusWasSet();
                });
            }
        }
    };

    handleItemDetail = (item, e) => {
        this.props.dispatch(extSystemDetailFetchIfNeeded(item.id));
    };

    renderListItem = (props) => {
        const { item } = props;
        return <AdminExtSystemListItem {...item} />;
    };

    render() {
        const { extSystemDetail, extSystemList } = this.props;

        let activeIndex = null;
        if (extSystemList.fetched && extSystemDetail.id !== null) {
            activeIndex = indexById(extSystemList.filteredRows, extSystemDetail.id);
        }

        let list;
        if (extSystemList.fetched) {
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
                list = <ul>
                    <li className="noResult">{i18n('search.action.noResult')}</li>
                </ul>;
            }

            // Wrap
            list = <div className="list">{list}</div>;
        }

        return <div className="ext-system-list">
            <StoreHorizontalLoader store={extSystemList}/>
            {list}
        </div>;
    }
}

export default connect((state) => {
    const { focus } = state;
    const extSystemList = storeFromArea(state, AREA_EXT_SYSTEM_LIST);
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);
    return {
        focus,
        extSystemList,
        extSystemDetail,
    };
})(AdminExtSystemList);
