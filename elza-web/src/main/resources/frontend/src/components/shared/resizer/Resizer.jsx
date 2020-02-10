import './Resizer.less'
import React from 'react';

export default class Resizer extends React.Component {

    render() {
        return (<span className={'Resizer ' + (this.props.horizontal ? "horizontal" : "vertical")} onMouseDown={this.props.onMouseDown} />);
    }
}

