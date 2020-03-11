import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import HorizontalPane from './HorizontalPane';
import AbstractReactComponent from '../../AbstractReactComponent';
import Resizer from '../resizer/Resizer';
import './HorizontalSplitter.scss';

class HorizontalSplitter extends AbstractReactComponent {

    state = {
        active: false,
        resized: false,
        dragged: false,
        position: null,
        size: this.props.bottomSize || 200
    };

    static propTypes = {
        top: PropTypes.node.isRequired,
        bottom: PropTypes.node.isRequired,
        bottomSize: PropTypes.number,
        onDragFinished: PropTypes.func,
        onChange: PropTypes.func,
    };

    componentDidMount() {
        document.addEventListener('mouseup', this.onMouseUp);
        document.addEventListener('mousemove', this.onMouseMove);

        this.updateChildPanes();
    }

    updateChildPanes = () => {
        const bottomRef = this.refs.paneBottom;
        if (bottomRef && this.state.bottomSize) {
            bottomRef.setState({
                size: this.state.size
            });
        }
    };

    UNSAFE_componentWillReceiveProps({size}) {
        if (size) {
            this.setState({size}, this.updateChildPanes)
        }
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.onMouseUp);
        document.removeEventListener('mousemove', this.onMouseMove);
    }

    unFocus = () => {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    };

    onMouseDown = (event) => {
        this.unFocus();
        let position = event.clientY;
        this.setState({
            active: true,
            position: position,
            dragged: true,
        });
    };

    onMouseMove = (event) => {
        if (this.state.active) {
            this.unFocus();
            const ref = this.state.dragged ? this.refs.paneBottom : null;
            if (ref) {
                const node = ReactDOM.findDOMNode(ref);
                if (node.getBoundingClientRect) {
                    //const width = node.getBoundingClientRect().width;
                    const height = node.getBoundingClientRect().height;
                    const current = event.clientY;
                    const size = height;
                    const position = this.state.position;

                    let newSize;
                    if (this.state.dragged) {
                        newSize = size - (current - position);
                    }
                    this.setState({
                        position: current,
                        resized: true
                    });

                    let newState = {};

                    if (this.state.dragged) {
                        newState = {
                            size: newSize,
                        }
                    }
                    if (this.props.onChange) {
                        this.props.onChange({size: newSize});
                    }
                    this.setState(newState);

                    ref.setState({
                        size: newSize
                    });
                }
            }
        }
    };

    onMouseUp = () => {
        if (this.state.active) {
            if (this.props.onDragFinished) {
                this.props.onDragFinished();
            }
            this.setState({
                active: false,
                dragged: false
            });
        }
    };

    render() {
        const {props: {top, bottom}} = this;

        return (
            <div ref='container' className='horizontal-splitter-container'>
                <div key='center' className='splitter-top'>{top}</div>
                <Resizer key='resizer' ref='resizer' horizontal onMouseDown={this.onMouseDown}/>
                <HorizontalPane key='right' ref='paneBottom' className='splitter-bottom'>{bottom}</HorizontalPane>
            </div>
        )
    }
}


export default HorizontalSplitter;

