import React from 'react';
import { AbstractReactComponent, FormInput, Icon } from 'components/shared';
import { WebApi } from 'actions/index.jsx';

import './AdminLogsDetail.scss';
import { Col, Row } from 'react-bootstrap';


/**
 * Komponenta detailu osoby
 */
class AdminLogsDetail extends AbstractReactComponent {

    constructor(props, context) {
        super(props, context);
        this.stop = false;
        this.state = {
            lineCount: 300,
            fetched: false,
            logs: [],
        };
    }

    componentDidMount() {
        WebApi.getLogs(this.state.lineCount, this.state.firstLine).then(data => {
            this.setState({ logs: data.lines, fetched: true }, () => {
                this.scrollDown();
                this.refresh();
            });
        }).catch(e => {
            this.setState({ logs: ['Chyba', e], fetched: true });
        });
    }

    componentWillUnmount() {
        this.stop = true;
    }

    pauseContinue = () => {
        if (this.stop) {
            this.stop = false;
            this.refresh();
            this.setState({});
        } else {
            this.stop = true;
            this.setState({});
        }
    };

    changeLineCount = (e) => {
        const value = e.target.value;
        if (value <= 0 || value > 10000) {
            return;
        }

        this.setState({
            ...this.state,
            lineCount: value,
            fetched: false,
            logs: [],
        }, () => {
            if (this.stop) {
                this.refresh(true);
            }
            this.scrollDown(); // TODO React 16 check
        });
    };

    isOnEnd = () => {
        if (this.refs.textLog) {
            const t = this.refs.textLog;
            if (t.scrollTop + t.offsetHeight >= t.scrollHeight) {
                return true;
            }
        }
        return false;
    };

    refresh = (force = false) => {
        const { lineCount, firstLine } = this.state;

        if (!force && this.stop) {
            return;
        }

        WebApi.getLogs(lineCount, firstLine).then(newData => {
            if (newData.lineCount > 0) {
                let scrollDown = false;

                if (this.isOnEnd()) {
                    scrollDown = true;
                }

                this.setState({
                    ...this.state,
                    fetched: true,
                    logs: newData.lines,
                }, () => {
                    if (scrollDown) {
                        this.scrollDown();
                    }
                    setTimeout(this.refresh, 1000);
                });
            } else {
                setTimeout(this.refresh, 3000);
            }
        }).catch(e => {
            this.setState({ logs: ['Chyba', e], fetched: true });
        });
    };

    scrollDown = () => {
        if (this.refs.textLog) {
            const t = this.refs.textLog;
            t.scrollTop = t.scrollHeight;
        }
    };

    componentDidUpdate() {

    }

    render() {
        const { logs, fetched, lineCount } = this.state;

        const isOnEnd = this.isOnEnd();

        let cls = 'btn';
        if (isOnEnd) {
            cls += ' active';
        }

        return <section className="logs-detail">
            <Row className="log-controll-buttons">
                <Col xs="1">
                    <div className="">
                        <button className={cls} onClick={this.scrollDown}>
                            <Icon glyph="fa-sort-desc"/>
                        </button>
                        <button className="btn"
                                onClick={this.pauseContinue}>{this.stop ? 'Pokračovat' : 'Pozastavit'}</button>
                    </div>
                </Col>
                <Col xs="3">
                    <FormInput type="number" value={lineCount} min="1" max="10000" onChange={this.changeLineCount}
                               label={false}/>
                </Col>
            </Row>
            <Row className="">
                <Col xs={12}>
                    <textarea readOnly onScroll={() => {
                        this.setState({});
                    }} spellCheck="false" ref="textLog"
                              className="logs" value={fetched ? logs.map(line => line + '\n').join('') : 'Načítání...'}>
                    </textarea>
                </Col>
            </Row>
        </section>;
    }
}

export default AdminLogsDetail;
