import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import Pane from './Pane'
import AbstractReactComponent from "../../AbstractReactComponent";
import Resizer from "../resizer/Resizer";
import './Splitter.scss'

class Splitter extends AbstractReactComponent {

    state = {
        active: false,
        resized: false,
        leftDragged: false,
        rightDragged: false,
        leftSize: this.props.leftSize || 200,
        rightSize: this.props.rightSize || 200
    };

    static propTypes = {
        left: PropTypes.object,
        leftSize: PropTypes.number,
        center: PropTypes.object.isRequired,
        right: PropTypes.object,
        rightSize: PropTypes.number,
        onDragFinished: PropTypes.func,
        onChange: PropTypes.func,
    };

    componentDidMount() {
        document.addEventListener('mouseup', this.onMouseUp);
        document.addEventListener('mousemove', this.onMouseMove);

        this.updateChildPanes();
    }

    updateChildPanes = () => {
        const leftRef = this.refs.paneLeft;
        if (leftRef && this.state.leftSize) {
            leftRef.setState({
                size: this.state.leftSize
            });
        }
        const rightRef = this.refs.paneRight;
        if (rightRef && this.state.rightSize) {
            rightRef.setState({
                size: this.state.rightSize
            });
        }
    };

    componentWillReceiveProps(nextProps) {
        if (nextProps.leftSize || nextProps.rightSize) {
            this.setState({
                leftSize: nextProps.leftSize || this.state.leftSize,
                rightSize: nextProps.rightSize || this.state.rightSize
            }, this.updateChildPanes)
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

    onMouseDownLeft = (event) => {
        this.unFocus();
        let position = event.clientX;
        this.setState({
            active: true,
            position: position,
            leftDragged: true,
        });
    };

    onMouseDownRight = (event) => {
        this.unFocus();
        let position = event.clientX;
        this.setState({
            active: true,
            position: position,
            rightDragged: true,
        });
    };

    onMouseMove = (event) => {
        if (this.state.active) {
            this.unFocus();
            const ref = this.state.leftDragged ? this.refs.paneLeft : this.state.rightDragged ? this.refs.paneRight : null;
            if (ref) {
                const node = ReactDOM.findDOMNode(ref);
                if (node.getBoundingClientRect) {
                    const width = node.getBoundingClientRect().width;
                    const height = node.getBoundingClientRect().height;
                    const current = event.clientX;
                    const size = width;
                    const position = this.state.position;

                    let newSize;
                    if (this.state.leftDragged) {
                        newSize = size - (position - current);
                    } else if (this.state.rightDragged) {
                        newSize = size - (current - position);
                    }
                    this.setState({
                        position: current,
                        resized: true
                    });

                    let newState = {};
                    if (this.state.leftDragged) {
                        newState = {
                            leftSize: newSize,
                            rightSize: this.state.rightSize
                        }
                    } else if (this.state.rightDragged) {
                        newState = {
                            leftSize: this.state.leftSize,
                            rightSize: newSize
                        }
                    }
                    if (this.props.onChange) {
                        this.props.onChange({leftSize: newState.leftSize, rightSize: newState.rightSize});
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
                leftDragged: false,
                rightDragged: false
            });
        }
    };

    render() {
        const {props: {left, right, center}} = this;

        const parts = [];

        if (left) {
            parts.push(<Pane key='left' ref='paneLeft' className='splitter-left'>{left}</Pane>);
            parts.push(<Resizer key='resizer-left' ref='resizerLeft' onMouseDown={this.onMouseDownLeft}/>);
        }

        parts.push(<div key='center' className='splitter-center'>{center}</div>);

        if (right) {
            parts.push(<Resizer key='resizer-right' ref='resizerRight' onMouseDown={this.onMouseDownRight}/>);
            parts.push(<Pane key='right' ref='paneRight' className='splitter-right'>{right}</Pane>);
        }

        return (
            <div ref='container' className='splitter-container'>
                {parts}
            </div>
        )
    }
}


export default connect(null, null, null, {withRef: true})(Splitter);

