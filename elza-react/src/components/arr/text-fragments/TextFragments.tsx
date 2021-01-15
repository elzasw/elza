import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx';
import {getOneSettings, setSettings} from 'components/arr/ArrUtils.jsx';
import {Icon, NoFocusButton} from 'components/shared';
import React, {ChangeEvent, FC, useEffect, useState} from 'react';
import {useDispatch, useSelector} from "react-redux";
import {i18n} from '../../../components/shared';
import {DraggableWindow} from "./DraggableWindow";
import "./TextFragments.scss";

let registeredField:any = null;
let callback: (char: string, field: any) => void;

export const registerField = (field: any, onChangeCallback: (char: string) => void) => {
    registeredField = field;
    callback = onChangeCallback;
}

export const unregisterField = () => {
    registeredField = null;
}

const handleClick = (e:any) => {
    callback && callback(e.currentTarget.textContent, registeredField);
}

const settingCode = "TEXT_FRAGMENTS"

const _fragments = "+\n$\n&\nU+03A3\nTest\nTrochu delsi text pro otestovani vkladani dlouheho textu.";

const delimiter = "\n"

interface ITextFragmentsSettings {
    fragments: string[];
}

export const TextFragmentsWindow:FC<{
    onClose: () => void
}> = ({
    onClose
}) => {
    const userDetail = useSelector((state:any)=>state.userDetail);
    const dispatch = useDispatch();
    const fragmentsSettings = getOneSettings(userDetail.settings, settingCode);
    let initFragments:string[] = [];

    try {
        const settings:ITextFragmentsSettings = JSON.parse(fragmentsSettings.value);
        if(!settings.fragments){
            throw new Error("Settings value is not object.")
        }
        initFragments = settings.fragments;
    } catch {
        initFragments = _fragments.split(delimiter);
    }

    const [disabled, setDisabled] = useState(true);
    const [editMode, setEditMode] = useState(false);
    const [fragments, setFragments] = useState<string[]>(initFragments);

    useEffect(()=>{ 
        if(registeredField){
            setDisabled(false);
        } else {
            setDisabled(true);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [registeredField])

    const handleToggleEdit = () => setEditMode(!editMode);

    const handleChangeFragmentsString = (e: ChangeEvent<HTMLTextAreaElement>) => {
        setFragments((e.currentTarget.value || "").split(delimiter))
    }

    const handleResetFragmentsString = () => {
        setFragments(JSON.parse(fragmentsSettings.value).fragments);
    }

    const handleSaveFragmentsString = () => {
        fragmentsSettings.value = JSON.stringify({fragments});
        const settings = setSettings(userDetail.settings, fragmentsSettings.id, fragmentsSettings);
        
        dispatch(userDetailsSaveSettings(settings))
    }

    const handleRemoveFragment = (fragment: string) => () => {
        const index = fragments.indexOf(fragment);
        const newFragments = [...fragments];
        newFragments.splice(index, 1)

        setFragments(newFragments);
    }

    return <DraggableWindow className="text-fragments-window">
        <div className="actions-container" >
            <div className="title">{i18n("textFragments.title")}</div>
            <div className="spacer"/>
            <div onMouseDown={(e)=>{e.stopPropagation()}}>
                <NoFocusButton active={editMode} onClick={handleToggleEdit}>
                    <Icon glyph="fa-pencil"/>
                </NoFocusButton>
                <NoFocusButton onClick={onClose}>
                    <Icon glyph="fa-times"/>
                </NoFocusButton>
            </div>
        </div>
        <div onMouseDown={(e)=>{e.stopPropagation();}} >
            <div className="scroll-window">
                <div className="item-container" >
                    {fragments.map((item, key)=>{
                        const char = item.startsWith("U+") ? String.fromCharCode(parseInt(item.slice(2),16)) : item;
                        return <button 
                            className={editMode ? "item edit" : "item"}
                            title={char}
                            disabled={disabled && !editMode}
                            key={key}
                            onClick={editMode ? handleRemoveFragment(item) : handleClick} 
                            onMouseDown={(event)=>{ 
                                event.preventDefault() 
                            }}
                        >
                            {char}
                            {editMode && <div className="delete-overlay"><Icon glyph="fa-trash"/></div>}
                        </button>
                    })}
                </div>
            </div>
            {editMode && 
            <div className="edit-form">
                    <textarea 
                        style={{
                            border:"1px solid #ddd",
                            backgroundColor: "#fff",
                            width:"100%"
                        }} 
                        onChange={handleChangeFragmentsString}
                        value={fragments.join(delimiter)}
                    />
                    <div className="actions-container">
                        <button onClick={handleSaveFragmentsString}>
                            <Icon glyph="fa-save"/>
                        </button>
                        <button onClick={handleResetFragmentsString}>
                            <Icon glyph="fa-undo"/>
                        </button>
                    </div>
            </div>}
        </div>
    </DraggableWindow>
}
