/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.
 */

import React from 'react';
import {AbstractReactComponent, AddRemoveList, Icon, i18n} from 'components/shared';

import './FundNodesList.less';
import {ApScopeVO} from "../../typings/Outputs";

type Props = {
    scopes: ApScopeVO[];
    onRemove: (item: ApScopeVO) => void;
    onAdd?: () => void;
    readOnly: boolean;
}

class ScopeList extends AbstractReactComponent<Props> {
    static propTypes = {
        scopes: React.PropTypes.array.isRequired,
        onRemove: React.PropTypes.func,
        onAdd: React.PropTypes.func,
        readOnly: React.PropTypes.bool
    };

    handleRenderItem = (props: ApScopeVO) => {
        const {item} = props;
        return <span>{item.name}</span>;
    };

    render() {
        const {scopes, onAdd, readOnly, ...other} = this.props;

        return (
            <AddRemoveList
                className="fund-nodes-list-container"
                readOnly={readOnly}
                items={scopes}
                onAdd={onAdd}
                addInLabel
                onRemove={this.handleDeleteItem}
                addTitle="arr.fund.nodes.title.select"
                removeTitle="arr.fund.nodes.title.remove"
                renderItem={this.handleRenderItem}
                {...other}
            />
        )
    }
}

export default ScopeList;
