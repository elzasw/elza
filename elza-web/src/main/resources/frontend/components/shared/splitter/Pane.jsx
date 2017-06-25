import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components/index.jsx';

require ('./Splitter.less')
var Resizer = require ('./Resizer')

var Pane = class Pane extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
        }
    }

    render() {
        let style = {
        }
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
