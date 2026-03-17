import { userDetailsSaveSettings } from 'actions/user/userDetail.jsx';
import { getOneSettings, setSettings } from 'components/arr/ArrUtils.jsx';
import { Icon, NoFocusButton } from 'components/shared';
import { ChangeEvent, useState } from 'react';
import { i18n } from '../../../components/shared';
import { DraggableWindow } from "components/shared";
import "./TextFragments.scss";
import { useAppThunkDispatch } from 'utils/hooks';
import { useTextFragmentsContext } from './TextFragmentsContext';
import { useAppSelector } from 'utils/hooks/useAppSelector';

let _textFragmentsWindowPosition = { x: window.innerWidth / 2, y: window.innerHeight / 2 };

let registeredField: any = null;
let callback: (char: string, field: any) => void;

export const registerField = (field: any, onChangeCallback: (char: string) => void) => {
    registeredField = field;
    callback = onChangeCallback;
}

export const unregisterField = () => {
    registeredField = null;
}

const settingCode = "TEXT_FRAGMENTS"

const _fragments = "+\n$\n&\nU+03A3\nTest\nTrochu delsi text pro otestovani vkladani dlouheho textu.";

const delimiter = "\n"

interface ITextFragmentsSettings {
    fragments: string[];
}

export function TextFragmentsWindow({ onClose }: { onClose: () => void }) {
    const settings = useAppSelector((state) => state.userDetail.settings);
    const dispatch = useAppThunkDispatch();
    const fragmentsSettings = getOneSettings(settings, settingCode);
    let initFragments: string[] = [];

    try {
        const settings: ITextFragmentsSettings = JSON.parse(fragmentsSettings.value);
        if (!settings.fragments) {
            throw new Error("Settings value is not object.")
        }
        initFragments = settings.fragments;
    } catch {
        initFragments = _fragments.split(delimiter);
    }

    const textFragments = useTextFragmentsContext();
    const disabled = !textFragments?.hasActiveField;

    const [editMode, setEditMode] = useState(false);
    const [fragments, setFragments] = useState<string[]>(initFragments);

    const handleToggleEdit = () => setEditMode(!editMode);

    const handleChangeFragmentsString = (e: ChangeEvent<HTMLTextAreaElement>) => {
        setFragments((e.currentTarget.value || "").split(delimiter))
    }

    const handleResetFragmentsString = () => {
        setFragments(JSON.parse(fragmentsSettings.value).fragments);
    }

    const handleSaveFragmentsString = () => {
        fragmentsSettings.value = JSON.stringify({ fragments });
        const _settings = setSettings(settings, fragmentsSettings.id, fragmentsSettings);
        dispatch(userDetailsSaveSettings(_settings))
    }

    const handleRemoveFragment = (fragment: string) => () => {
        const index = fragments.indexOf(fragment);
        const newFragments = [...fragments];
        newFragments.splice(index, 1)
        setFragments(newFragments);
    }

    return <div onMouseDown={(e) => e.preventDefault()}>
        <DraggableWindow
            className="text-fragments-window"
            initialPosition={_textFragmentsWindowPosition}
            onDragStop={(position) => { _textFragmentsWindowPosition = position }}
            dragWholeWindow={true}
        >
            <div className="actions-container" >
                <div className="title">{i18n("textFragments.title")}</div>
                <div className="spacer" />
                <div onMouseDown={(e) => { e.stopPropagation(); e.preventDefault() }}>
                    <NoFocusButton active={editMode} onClick={handleToggleEdit}>
                        <Icon glyph="fa-pencil" />
                    </NoFocusButton>
                    <NoFocusButton onClick={onClose}>
                        <Icon glyph="fa-times" />
                    </NoFocusButton>
                </div>
            </div>
            <div onMouseDown={(e) => { e.stopPropagation(); }} >
                <div className="scroll-window">
                    <div className="item-container" >
                        {fragments.map((item, key) => {
                            const char = item.startsWith("U+") ? String.fromCharCode(parseInt(item.slice(2), 16)) : item;
                            return <button
                                className={editMode ? "item edit" : "item"}
                                title={char}
                                disabled={disabled && !editMode}
                                key={key}
                                onClick={editMode ? handleRemoveFragment(item) : () => textFragments?.insertText(char)}
                                onMouseDown={(event) => {
                                    event.preventDefault()
                                }}
                            >
                                {char}
                                {editMode && <div className="delete-overlay"><Icon glyph="fa-trash" /></div>}
                            </button>
                        })}
                    </div>
                </div>
                {editMode &&
                    <div className="edit-form">
                        <textarea
                        onChange={handleChangeFragmentsString}
                        value={fragments.join(delimiter)}
                        />
                        <div className="actions-container">
                            <button onClick={handleSaveFragmentsString}>
                                <Icon glyph="fa-save" />
                            </button>
                            <button onClick={handleResetFragmentsString}>
                                <Icon glyph="fa-undo" />
                            </button>
                        </div>
                    </div>}
            </div>
        </DraggableWindow>
    </div>
}
