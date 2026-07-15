import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import {Button, SpinButton, Tooltip} from '@fluentui/react-components';
import {ArrowDownRegular, PauseRegular, PlayRegular} from '@fluentui/react-icons';

import './AdminLogsDetail.scss';

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
        WebApi.getLogs(this.state.lineCount, this.state.firstLine)
            .then(data => {
                this.setState({logs: data.lines, fetched: true}, () => {
                    this.scrollDown();
                    this.refresh();
                });
            })
            .catch(e => {
                this.setState({logs: ['Chyba', e], fetched: true});
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

    changeLineCount = (_event, data) => {
        const value = data.value ?? Number(data.displayValue);
        if (isNaN(value) || value <= 0 || value > 10000) {
            return;
        }

        this.setState(
            {
                ...this.state,
                lineCount: value,
                fetched: false,
                logs: [],
            },
            () => {
                if (this.stop) {
                    this.refresh(true);
                }
                this.scrollDown(); // TODO React 16 check
            },
        );
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
        const {lineCount, firstLine} = this.state;

        if (!force && this.stop) {
            return;
        }

        WebApi.getLogs(lineCount, firstLine)
            .then(newData => {
                if (newData.lineCount > 0) {
                    let scrollDown = false;

                    if (this.isOnEnd()) {
                        scrollDown = true;
                    }

                    this.setState(
                        {
                            ...this.state,
                            fetched: true,
                            logs: newData.lines,
                        },
                        () => {
                            if (scrollDown) {
                                this.scrollDown();
                            }
                            setTimeout(this.refresh, 1000);
                        },
                    );
                } else {
                    setTimeout(this.refresh, 3000);
                }
            })
            .catch(e => {
                this.setState({logs: ['Chyba', e], fetched: true});
            });
    };

    scrollDown = (smooth = false) => {
        if (this.refs.textLog) {
            const t = this.refs.textLog;
            t.scrollTo({top: t.scrollHeight, behavior: smooth ? 'smooth' : 'auto'});
        }
    };

    componentDidUpdate() {}

    render() {
        const {logs, fetched, lineCount} = this.state;

        const isOnEnd = this.isOnEnd();

        return (
            <section className="logs-detail">
                <div className="log-controll-buttons">
                    <Tooltip content="Posunout dolů" relationship="label" withArrow>
                        <Button
                            appearance="outline"
                            disabled={isOnEnd}
                            icon={<ArrowDownRegular />}
                            onClick={() => this.scrollDown(true)}
                        />
                    </Tooltip>
                    <Tooltip
                        content={this.stop ? 'Pokračovat' : 'Pozastavit'}
                        relationship="label"
                        withArrow
                    >
                        <Button
                            icon={this.stop ? <PlayRegular /> : <PauseRegular />}
                            onClick={this.pauseContinue}
                        />
                    </Tooltip>
                    <SpinButton
                        value={lineCount}
                        min={1}
                        max={10000}
                        onChange={this.changeLineCount}
                    />
                </div>
                <div className="logs-readout">
                    <textarea
                        readOnly
                        onScroll={() => {
                            this.setState({});
                        }}
                        spellCheck="false"
                        ref="textLog"
                        className="logs"
                        value={fetched ? logs.map(line => line + '\n').join('') : 'Načítání...'}
                    ></textarea>
                </div>
            </section>
        );
    }
}

export default AdminLogsDetail;
