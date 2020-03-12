/*
    Komponenta pro zobrazování dlouhého textu, který bude zkrácen a přidán odkaz na zobrazení celého textu,
    Např. "Asjh sdjkfh... (více)". Pokud není uveden maximální počet znaků pro zobrazení, je použita implicitní
    hodnota 164.
    Komponenta využívat klíč 'global.action.show.more' z messages.
*/
import PropTypes from 'prop-types';

import * as React from 'react';
import i18n from './i18n';
import {Button} from './ui';

export default class LongText extends React.Component {
    static propTypes = {
        // Maximální počet znaků pro zobrazení, pokud bude mít text více, bude zkrácen. Implicitně 164.
        max: PropTypes.number,
    };
    state = { expanded: false };

    handleShowMore = () => this.setState({ expanded: true });

    render() {
        let text = this.props.text;
        let more = null;

        if (!this.state.expanded) {
            const max = this.props.max || 164;
            if (text.length > max) {
                text = text.substring(0, max-3) + "...";
                more = <span> (<Button variant="link" onClick={this.handleShowMore}>{i18n('global.action.show.more')}</Button>)</span>
            }
        }

        return (
            <span>
                {text}{more}
            </span>
        );
    }
}
