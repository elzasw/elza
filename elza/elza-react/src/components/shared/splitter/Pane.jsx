import React from 'react';
import './Splitter.scss';
import AbstractReactComponent from '../../AbstractReactComponent';

/**
 * Pane
 * state je upravován z venku pomocí ref
 */
class Pane extends AbstractReactComponent {
    state = {};

    render() {
        let style = {};
        if (this.state.size) {
            style.width = this.state.size;
        }
        return (
            <div className={this.props.className} style={style}>
                {this.props.children}
            </div>
        );
    }
}

export default Pane;
