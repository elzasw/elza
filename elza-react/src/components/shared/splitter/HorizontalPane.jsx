import React from 'react';
import ReactDOM from 'react-dom';
import './Splitter.less'
import AbstractReactComponent from "../../AbstractReactComponent";

/**
 * Pane
 * state je upravován z venku pomocí ref
 */
class HorizontalPane extends AbstractReactComponent {

    state = {};

    render() {
        let style = {};
        if (this.state.size) {
            style.height = this.state.size;
        }
        return (
            <div className={this.props.className} style={style}>
                {this.props.children}
            </div>
        )
    }
}

export default HorizontalPane;
