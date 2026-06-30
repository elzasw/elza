import React from 'react';
import { connect } from 'react-redux';
import { Button, Combobox, Option, Radio, RadioGroup, Spinner } from '@fluentui/react-components';
import { Api } from '../../api';
import type { BulkAction, FundsActionGroup, FundsActionGroupResult, MultiFundActionResult } from 'elza-api';
import { modalDialogHide } from '../../actions/global/modalDialog';

/**
 * Průvodce spuštěním hromadné akce nad více archivními soubory najednou.
 *
 * Kroky: načtení skupin podle pravidel → (volba pravidel, je-li jich víc) → volba akce
 * → potvrzení → spuštění → výsledek.
 */

type Stage = 'loading' | 'empty' | 'chooseRuleSet' | 'chooseAction' | 'confirm' | 'submitting' | 'done';

interface Props {
    /** Identifikátory vybraných fondů. */
    fundIds: number[];
    dispatch?: any;
}

interface State {
    stage: Stage;
    groupResult?: FundsActionGroupResult;
    selectedRuleSetId?: number;
    selectedActionCode?: string;
    queueResult?: MultiFundActionResult;
}

class MultiFundActionDialog extends React.Component<Props, State> {
    state: State = { stage: 'loading' };

    componentDidMount() {
        Api.funds
            .bulkActionGroupFundsByRuleSet({ fundIds: this.props.fundIds })
            .then(({ data }) => {
                const groups = data.groups || [];
                if (groups.length === 0) {
                    this.setState({ stage: 'empty', groupResult: data });
                } else if (groups.length === 1) {
                    this.setState({ stage: 'chooseAction', groupResult: data, selectedRuleSetId: groups[0].ruleSetId });
                } else {
                    this.setState({ stage: 'chooseRuleSet', groupResult: data });
                }
            })
            .catch(() => this.close());
    }

    close = () => {
        this.props.dispatch(modalDialogHide());
    };

    selectedGroup(): FundsActionGroup | undefined {
        return this.state.groupResult?.groups?.find(g => g.ruleSetId === this.state.selectedRuleSetId);
    }

    selectedAction(): BulkAction | undefined {
        return this.selectedGroup()?.actions?.find(a => a.code === this.state.selectedActionCode);
    }

    handleConfirm = () => {
        const group = this.selectedGroup();
        const action = this.selectedAction();
        if (!group || !action) {
            return;
        }
        this.setState({ stage: 'submitting' });
        Api.funds
            .bulkActionQueueMultiFundAction({ fundVersionIds: group.fundVersionIds, code: action.code })
            .then(({ data }) => this.setState({ stage: 'done', queueResult: data }))
            .catch(() => this.close());
    };

    renderSkipped(skipped: FundsActionGroupResult['skipped']) {
        if (!skipped || skipped.length === 0) {
            return null;
        }
        return (
            <div className="muted" style={{ marginTop: 10, fontSize: '0.9em' }}>
                Přeskočeno {skipped.length} fondů (bez otevřené verze nebo akce nepatří do jejich pravidel).
            </div>
        );
    }

    render() {
        const { stage, groupResult } = this.state;

        if (stage === 'loading' || stage === 'submitting') {
            return (
                <div style={{ padding: 10 }}>
                    <Spinner label={stage === 'loading' ? 'Načítání fondů…' : 'Spouštění akce…'} />
                </div>
            );
        }

        if (stage === 'empty') {
            return (
                <div style={{ padding: 10 }}>
                    <div>Pro vybrané fondy není k dispozici žádná použitelná hromadná akce.</div>
                    {this.renderSkipped(groupResult?.skipped)}
                    <div style={{ marginTop: 15, textAlign: 'right' }}>
                        <Button onClick={this.close}>Zavřít</Button>
                    </div>
                </div>
            );
        }

        if (stage === 'chooseRuleSet') {
            const groups = groupResult?.groups || [];
            return (
                <div style={{ padding: 10 }}>
                    <div style={{ marginBottom: 8 }}>
                        Vybrané fondy používají více sad pravidel. Zvolte, pro kterou skupinu chcete akci spustit:
                    </div>
                    <RadioGroup
                        value={this.state.selectedRuleSetId != null ? String(this.state.selectedRuleSetId) : undefined}
                        onChange={(_e, d) => this.setState({ selectedRuleSetId: Number(d.value) })}
                    >
                        {groups.map(g => (
                            <Radio
                                key={g.ruleSetId}
                                value={String(g.ruleSetId)}
                                label={`${g.ruleSetName} (${g.fundCount} fondů)`}
                            />
                        ))}
                    </RadioGroup>
                    {this.renderSkipped(groupResult?.skipped)}
                    <div style={{ marginTop: 15, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                        <Button onClick={this.close}>Zrušit</Button>
                        <Button
                            appearance="primary"
                            disabled={this.state.selectedRuleSetId == null}
                            onClick={() => this.setState({ stage: 'chooseAction' })}
                        >
                            Pokračovat
                        </Button>
                    </div>
                </div>
            );
        }

        if (stage === 'chooseAction') {
            const group = this.selectedGroup();
            const actions = group?.actions || [];
            return (
                <div style={{ padding: 10 }}>
                    <div style={{ marginBottom: 8 }}>
                        Akce bude spuštěna nad {group?.fundCount} fondy (pravidla {group?.ruleSetName}). Zvolte akci:
                    </div>
                    {actions.length === 0 ? (
                        <div className="muted">Pro tato pravidla není dostupná žádná hromadná akce.</div>
                    ) : (
                        <Combobox
                            aria-label="Hromadná akce"
                            placeholder="Vyberte akci…"
                            style={{ width: '100%' }}
                            selectedOptions={this.state.selectedActionCode ? [this.state.selectedActionCode] : []}
                            value={this.selectedAction()?.name ?? ''}
                            onOptionSelect={(_e, d) => this.setState({ selectedActionCode: d.optionValue })}
                        >
                            {actions.map(a => (
                                <Option key={a.code} value={a.code} text={a.name}>
                                    {a.name}
                                </Option>
                            ))}
                        </Combobox>
                    )}
                    <div style={{ marginTop: 15, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                        <Button onClick={this.close}>Zrušit</Button>
                        <Button
                            appearance="primary"
                            disabled={this.state.selectedActionCode == null}
                            onClick={() => this.setState({ stage: 'confirm' })}
                        >
                            Pokračovat
                        </Button>
                    </div>
                </div>
            );
        }

        if (stage === 'confirm') {
            const group = this.selectedGroup();
            const action = this.selectedAction();
            return (
                <div style={{ padding: 10 }}>
                    <div>
                        Akce <b>{action?.name}</b> bude spuštěna nad <b>{group?.fundCount}</b> fondy. Akce zapisuje
                        do dat fondů a každý fond získá samostatný běh. Pokračovat?
                    </div>
                    {this.renderSkipped(groupResult?.skipped)}
                    <div style={{ marginTop: 15, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                        <Button onClick={this.close}>Zrušit</Button>
                        <Button appearance="primary" onClick={this.handleConfirm}>
                            Spustit
                        </Button>
                    </div>
                </div>
            );
        }

        // stage === 'done'
        const queueResult = this.state.queueResult;
        const skippedCount = queueResult?.skipped?.length || 0;
        return (
            <div style={{ padding: 10 }}>
                <div>
                    Hromadná akce byla naplánována pro <b>{queueResult?.queuedCount}</b> fondů.
                    {skippedCount > 0 && <> Přeskočeno {skippedCount} fondů.</>}
                </div>
                <div className="muted" style={{ marginTop: 8, fontSize: '0.9em' }}>
                    Průběh jednotlivých běhů lze sledovat ve správě úloh.
                </div>
                <div style={{ marginTop: 15, textAlign: 'right' }}>
                    <Button appearance="primary" onClick={this.close}>
                        Zavřít
                    </Button>
                </div>
            </div>
        );
    }
}

export default connect()(MultiFundActionDialog);
