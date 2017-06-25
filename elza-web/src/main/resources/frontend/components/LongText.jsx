/*
    Komponenta pro zobrazování dlouhého textu, který bude zkrácen a přidán odkaz na zobrazení celého textu,
    Např. "Asjh sdjkfh... (více)". Pokud není uveden maximální počet znaků pro zobrazení, je použita implicitní
    hodnota 164.
    Komponenta využívat klíč 'global.action.show.more' z messages.
*/

var React = require('react');
var i18n = require('./i18n');

export default React.createClass({
    propTypes: {
        // Maximální počet znaků pro zobrazení, pokud bude mít text více, bude zkrácen. Implicitně 164.
        max: React.PropTypes.number,
    },
    getInitialState: function() {
        return { expanded: false };
    },
    handleShowMore: function() {
        this.setState({ expanded: true });
    },
    render: function() {
        var text = this.props.text;
        var more=null;

        if (!this.state.expanded) {
            var max = this.props.max || 164;
                if (text.length > max) {
                    text = text.substring(0, max-3) + "...";
                    more = <span> (<a href="#" onClick={this.handleShowMore}>{i18n('global.action.show.more')}</a>)</span>
                }
        }

        return (
            <span>
                {text}{more}
            </span>
        );
    }
});
