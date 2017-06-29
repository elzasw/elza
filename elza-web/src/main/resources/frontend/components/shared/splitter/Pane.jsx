import React from 'react';
import ReactDOM from 'react-dom';
import './Splitter.less'
import AbstractReactComponent from "../../AbstractReactComponent";

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
        )
    }
}

export default Pane;
