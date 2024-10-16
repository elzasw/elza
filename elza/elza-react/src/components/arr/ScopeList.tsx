/**
 * Komponenta seznamu vybraných uzlů pro verzi archivního souboru.
 */

import React from 'react';
import PropTypes from 'prop-types';
import {AbstractReactComponent, AddRemoveList, Icon, i18n} from 'components/shared';

import './FundNodesList.scss';
import {ApScopeVO} from '../../typings/Outputs';
import {Button} from '../ui';

type Props = {
    scopes: ApScopeVO[];
    onRemove: (item: ApScopeVO) => void;
    onAdd?: () => void;
    readOnly: boolean;
};

export class ScopeList extends AbstractReactComponent<Props> {
    static propTypes = {
        scopes: PropTypes.array.isRequired,
        onRemove: PropTypes.func,
        onAdd: PropTypes.func,
        readOnly: PropTypes.bool,
    };

    handleRenderItem = props => {
        const {item} = props;
        return (
            <span>
                {item.name}
                {!this.props.readOnly && this.props.onRemove && (
                    <Button variant={'action'} onClick={() => this.props.onRemove(item)} className={'ml-auto'}>
                        <Icon glyph={'fa-trash'} />
                    </Button>
                )}
            </span>
        );
    };

    render() {
        const {scopes, onAdd, readOnly, onRemove, ...other} = this.props;

        return (
            <AddRemoveList
                className="fund-nodes-list-container"
                readOnly={readOnly}
                items={scopes}
                onAdd={onAdd}
                addInLabel
                onRemove={onRemove}
                addTitle="arr.fund.nodes.title.select"
                removeTitle="arr.fund.nodes.title.remove"
                renderItem={this.handleRenderItem}
                {...other}
            />
        );
    }
}
