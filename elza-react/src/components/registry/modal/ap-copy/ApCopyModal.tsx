import { ApAccessPointVO } from 'api';
import { ApItemVO } from 'api/ApItemVO';
import { ApPartVO } from 'api/ApPartVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import classNames from 'classnames';
import { sortPart } from 'components/registry/ApDetailPageWrapper';
import { FormScope } from 'components/registry/part-edit/form/fields';
import { FormInput } from 'components/shared';
import React, { ChangeEvent } from 'react';
import { Modal } from 'react-bootstrap';
import { Field, Form } from 'react-final-form';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import i18n from "../../../i18n";
import { Button } from "../../../ui";
import './ApCopyModal.scss';

interface ApCopyModalFields {
    scope: string;
    replace: boolean;
    skipParts: number[];
    skipItems: number[];
}

type ApCopyModalResult = Omit<ApCopyModalFields,'skipParts'>;

type Props = {
    onClose: () => void;
    onSubmit: (data: ApCopyModalResult) => Promise<void>;
    detail: ApAccessPointVO;
}

interface ApPartWithRelated extends ApPartVO {
    relatedParts?: ApPartVO[];
}

const getItemValue = (item: ApItemVO | any, itemType: RulDescItemTypeExtVO) => {
    const itemSpec = itemType.descItemSpecs.find((spec) => spec.id === item.specId)
    if(itemSpec && !item.value){
        return itemSpec.name;
    } 
    if(item.accessPoint){
        return `${itemSpec?.name}: ${item.accessPoint.name}`;
    }
    if(item.description){
        return `${item.description}: ${item.value}`;
    }
    return item.value;
}

interface ItemProps {
    item: ApItemVO;
    checked: boolean;
    disabled: boolean;
    onChange: (event: ChangeEvent<HTMLInputElement> | React.MouseEvent) => void;
}
const Item = ({item, checked, disabled, onChange}: ItemProps) => {
    const {descItemTypes} = useSelector((state: AppState) => state.refTables);
    const itemType = descItemTypes.itemsMap[item.typeId];

    // console.log("#### item type", itemType)

    return <div className={classNames({"item":true, "muted": checked})}>
        <input 
            type="checkbox" 
            checked={!checked} 
            disabled={disabled} 
            onChange={onChange}
            style={{marginRight: "10px"}}
        />
        <div style={{lineHeight: "1em", cursor: "pointer"}} onClick={onChange}>
            <div style={{fontSize: "0.9em"}}>
                <b>{itemType.name}</b>
            </div>
            <div style={{fontSize: "1.1em", marginTop: "2px"}}>
                {getItemValue(item, itemType)}
            </div>
        </div>
    </div>
}

export const ApCopyModal = ({
    onClose,
    onSubmit,
    detail,
}:Props) => {
    const {scopesData, partTypes, descItemTypes} = useSelector((state: AppState) => state.refTables);
    const apViewSettings:any = useSelector((state: AppState) => state.app.apViewSettings);


    const getRelatedParts = (parts: ApPartVO[]) => {
        const partsWithRelated:ApPartWithRelated[] = [];
        const relatedParts:ApPartVO[] = [];
        // const relatedParts:Record<number, ApPartWithRelated[]> = {};
        parts.forEach((part) => {
            if(part.partParentId == undefined){
                partsWithRelated.push(part);
            } else {
                relatedParts.push(part);
            }
        })

        relatedParts.forEach((relatedPart) => {
            const partIndex = partsWithRelated.findIndex((part) => part.id === relatedPart.partParentId);
            if(partIndex != undefined){
                if(!partsWithRelated[partIndex].relatedParts){
                    partsWithRelated[partIndex].relatedParts = [relatedPart];
                }
                else if(!partsWithRelated[partIndex].relatedParts?.find((_relatedPart) => _relatedPart.id === relatedPart.id)){
                    partsWithRelated[partIndex].relatedParts?.push(relatedPart);
                }
            }
        })

        return partsWithRelated;
    }

    const groupPartsByType = (data: ApPartWithRelated[]) => {
        return data.reduce<Record<string, ApPartWithRelated[]>>((accumulator, value) => {
            const typeId = value.typeId;
            if(typeId != undefined){
                const currentValue = accumulator[typeId] || [];
                accumulator[typeId.toString()] = [...currentValue, value];
            }
            return accumulator;
        }, {});
    }

    const groupedParts = groupPartsByType(getRelatedParts(detail.parts));


    const sortedPartTypes = detail && partTypes.items
        ? sortPart(partTypes.items, apViewSettings.data?.rules[detail.ruleSetId])
        : [];

    const validate = (data: ApCopyModalFields) => {
        const errors:Partial<Record<keyof ApCopyModalFields, string>> = {};
        if(data.scope == undefined){
            errors.scope = "Neni vyplnena oblast"
        }
        return errors;
    }

    const handleSubmit = ({scope, replace, skipParts, skipItems}: ApCopyModalFields) => {
        const _skipItems:number[] = [...skipItems];

        // find and add items in skipped parts
        skipParts.forEach((skippedPartId) => {
            const part = detail.parts.find((part) => part.id === skippedPartId);
            if(part){
                part.items?.forEach((item) => {
                    const isItemSkipped = _skipItems.findIndex((skippedItemId) => skippedItemId === item.id) >= 0;
                    if(!isItemSkipped && item.id != undefined){
                        _skipItems.push(item.id);
                    }
                })
                const relatedParts = detail.parts.filter(({partParentId}) => partParentId === part.id);
                relatedParts?.forEach((relatedPart) => {
                    relatedPart.items?.forEach((item) => {
                        const isItemSkipped = _skipItems.findIndex((skippedItemId) => skippedItemId === item.id) >= 0;
                        if(!isItemSkipped && item.id != undefined){
                            _skipItems.push(item.id);
                        }
                    })
                })
            }
        })
        
        onSubmit({
            scope: parseInt(scope, 10) as any,
            replace,
            skipItems: _skipItems,
        })
    }

    return <Form<ApCopyModalFields> onSubmit={handleSubmit} validate={validate} initialValues={{replace: true, skipParts: [], skipItems: []}}>
        {({submitting, handleSubmit, form, valid}) => {
            return <>
                <Modal.Body className="ap-copy-modal">
                    <FormScope name="scope" label={i18n("ap.copy.scope")} items={scopesData.scopes}/>
                    <div style={{marginTop: "10px"}}>
                        <Field<boolean> name="replace">
                            {(props) => {
                                return <div style={{display: "flex", alignItems: "center"}}>
                                    <FormInput type="checkbox" {...props.input} checked={props.input.value} />
                                    <span>{i18n("ap.copy.replace")}</span>
                                </div>
                            }}
                        </Field>
                    </div>
                    <hr/>
                    <Field<number[]> name="skipParts">{({input: skipPartsInput}) => {
                        const partsArray = skipPartsInput.value;

                        const handleChangePart = (id: number) => (e: ChangeEvent | React.MouseEvent) => {
                            console.log("#### handle change part", e)
                            const index = partsArray.indexOf(id);
                            if(index < 0){
                                partsArray.push(id);
                            }
                            else {
                                partsArray.splice(index, 1);
                            }
                            skipPartsInput.onChange(e);
                            form.change("skipParts", partsArray);
                        }

                        return <Field<number[]> name="skipItems">
                            {({input: skipItemsInput}) => {
                                const itemsArray = skipItemsInput.value;

                                const handleChangeItem = (id: number) => (e:React.ChangeEvent | React.MouseEvent) => {
                                    const index = itemsArray.indexOf(id);
                                    if(index < 0){
                                        itemsArray.push(id);
                                    }
                                    else {
                                        itemsArray.splice(index, 1);
                                    }
                                    skipItemsInput.onChange(e);
                                    form.change("skipItems", itemsArray);
                                }

                                const getSkippedItemsInArray = (items?: ApItemVO[] | null) => {
                                    if(!items){return [];}
                                    return items.filter((item) => {
                                        return itemsArray.findIndex((itemId) => itemId === item.id) >= 0;
                                    })
                                }

                                return <div className="list">
                                    {sortedPartTypes.map((partType) => {
                                        const parts = groupedParts[partType.id] || [];

                                        if(parts.length === 0){
                                            return <></>;
                                        }

                                        return <div>
                                            <div>
                                                <b>{partType.name}</b>
                                            </div>
                                            <div>
                                                {parts.map((part)=>{
                                                    const isEveryRelatedPartSkipped = part.relatedParts?.filter((relatedPart) => {
                                                        const isRelatedPartSkipped = partsArray.findIndex((partId) => partId === relatedPart.id) >= 0;
                                                        const isEveryRelatedPartItemSkipped = getSkippedItemsInArray(relatedPart.items).length === relatedPart.items?.length;

                                                        return isRelatedPartSkipped || isEveryRelatedPartItemSkipped;
                                                    }).length === part.relatedParts?.length;

                                                    const isEveryItemSkipped = getSkippedItemsInArray(part.items).length === part.items?.length && isEveryRelatedPartSkipped;
                                                    const isPartSkipped = partsArray.indexOf(part.id) >= 0;

                                                    return <div 
                                                        key={part.id} 
                                                        className={classNames({"part": true, "muted": isPartSkipped || isEveryItemSkipped})}
                                                    >
                                                        <div className="title" onClick={handleChangePart(part.id)}>
                                                            <input 
                                                                type="checkbox" 
                                                                checked={!isPartSkipped} 
                                                                style={{marginRight: "10px"}}
                                                            />
                                                            <b>{part.value}</b>
                                                        </div>
                                                        {!isPartSkipped && <>
                                                            <div className="item-list">
                                                                {part.items?.map((item:any) => {
                                                                    const isItemSkipped = itemsArray.indexOf(item.id) >= 0 || isPartSkipped;
                                                                    return <Item 
                                                                        key={item.id} 
                                                                        item={item} 
                                                                        disabled={isPartSkipped} 
                                                                        checked={isItemSkipped} 
                                                                        onChange={handleChangeItem(item.id)}
                                                                    />
                                                                })}
                                                            </div>
                                                            {part.relatedParts?.map((relatedPart)=>{
                                                                const isEveryItemSkipped = getSkippedItemsInArray(relatedPart.items).length === relatedPart.items?.length;
                                                                const isRelatedPartSkipped = partsArray.indexOf(relatedPart.id) >= 0 || isPartSkipped;

                                                                return <div 
                                                                    key={relatedPart.id} 
                                                                    className={classNames({"related-part":true, "muted": isRelatedPartSkipped || isEveryItemSkipped})}
                                                                >
                                                                    <div className="title" onClick={handleChangePart(relatedPart.id)}>
                                                                        <input 
                                                                            type="checkbox" 
                                                                            checked={!isRelatedPartSkipped} 
                                                                            style={{marginRight: "10px"}}
                                                                            disabled={isPartSkipped}
                                                                        />
                                                                        <b>{relatedPart.value}</b>
                                                                    </div>
                                                                    {!isRelatedPartSkipped && <>
                                                                        <div className="item-list">
                                                                            {relatedPart.items?.map((item:any) => {
                                                                                const isItemSkipped = itemsArray.indexOf(item.id) >= 0 || isRelatedPartSkipped;
                                                                                return <Item 
                                                                                    key={item.id} 
                                                                                    item={item} 
                                                                                    disabled={isRelatedPartSkipped} 
                                                                                    checked={isItemSkipped} 
                                                                                    onChange={handleChangeItem(item.id)}
                                                                                />
                                                                            })}
                                                                        </div>
                                                                    </>}
                                                                </div>
                                                            })}
                                                        </>}
                                                    </div>
                                                })}
                                            </div>
                                        </div>
                                    })}
                                </div>
                            }}
                        </Field>
                    }}
                    </Field>
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={submitting || !valid} onClick={handleSubmit} variant="outline-secondary">{i18n('global.action.write')}</Button>
                    <Button variant="link" onClick={onClose} disabled={submitting}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </>
        }} 
    </Form>;
}
