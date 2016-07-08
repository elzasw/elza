import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {Button} from 'react-bootstrap';

require ('./ArrPanel.less');

var ArrPanel = class ArrPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleReset');
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    handleReset() {
        if (this.props.onReset) {
            this.props.onReset();
        } else {
            console.error("onReset not defined");
        }
    }

    render() {
        return (
                <div key='arr-panel' className='arr-panel'>
                    <Button className="reset-button" title={i18n('arr.panel.reset')} onClick={this.handleReset}><Icon glyph="fa-times" /></Button>
                    <span className="title">{i18n('arr.panel.title', this.props.name)}</span>
                </div>
        );
    }
}

ArrPanel.propTypes = {
    name: React.PropTypes.string.isRequired,
    onReset: React.PropTypes.func.isRequired
}

module.exports = connect()(ArrPanel);
