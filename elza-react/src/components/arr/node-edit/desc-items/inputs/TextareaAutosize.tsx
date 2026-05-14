import { Textarea, TextareaProps } from "@fluentui/react-components";
import { useRef, useState } from "react";
import { FIELD_HEIGHT } from "../../../../../constants";
import { useDebouncedLayoutEffect } from "../../../../../utils/hooks/hooks";

interface Props extends TextareaProps { }

export function TextareaAutosize({ value, ...otherProps }: Props) {
  const fieldRef = useRef<HTMLTextAreaElement>(null);
  const wrapperRef = useRef<HTMLSpanElement>(null);
  const [textAreaHeight, setTextAreaHeight] = useState<number>(0);
  const [innerMinHeight, setInnerMinHeight] = useState<number>(0);
  const [verticalPadding, setVerticalPadding] = useState<number>(0);
  const [defaultPadBottom, setDefaultPadBottom] = useState<number>(0);

  const fieldHeight = otherProps.size === "small" ? FIELD_HEIGHT.small : FIELD_HEIGHT.medium;
  const contentAreaMin = innerMinHeight - verticalPadding - defaultPadBottom;

  function resizeTextarea(fieldHeight: number, contentAreaMin: number) {
    let wrapperPadding = 0;
    if (wrapperRef.current) {
      const computed = getComputedStyle(wrapperRef.current);
      wrapperPadding =
        parseFloat(computed.paddingTop) +
        parseFloat(computed.paddingBottom) +
        parseFloat(computed.borderTopWidth) +
        parseFloat(computed.borderBottomWidth);
    }
    const computedInnerMinHeight = fieldHeight - wrapperPadding;
    setInnerMinHeight(computedInnerMinHeight);

    const textarea = fieldRef.current;
    if (!textarea) return;

    const computed = getComputedStyle(textarea);
    const lineHeight = parseFloat(computed.lineHeight) || parseFloat(computed.fontSize) * 1.2;
    const paddingBottom = parseFloat(computed.paddingBottom);
    setDefaultPadBottom(paddingBottom);
    const computedVerticalPadding = Math.max(0, (computedInnerMinHeight - lineHeight) / 2 - paddingBottom);
    setVerticalPadding(computedVerticalPadding);

    const prevPaddingTop = textarea.style.paddingTop;
    const prevPaddingBottom = textarea.style.paddingBottom;
    textarea.style.paddingTop = "0";
    textarea.style.paddingBottom = "0";
    textarea.style.minHeight = `${contentAreaMin}px`;
    textarea.style.height = "0";
    const scrollHeight = textarea.scrollHeight + 2;
    textarea.style.height = `${scrollHeight}px`;
    textarea.style.paddingTop = prevPaddingTop;
    textarea.style.paddingBottom = prevPaddingBottom;
    textarea.style.minHeight = `${contentAreaMin}px`;
    setTextAreaHeight(scrollHeight);
  }

  useDebouncedLayoutEffect(
    () => {
      resizeTextarea(fieldHeight, contentAreaMin);
    },
    50,
    [value, otherProps.size, fieldHeight, contentAreaMin],
  );

  const contentAreaHeight = Math.max(textAreaHeight, contentAreaMin);

  return (
    <Textarea
      {...otherProps}
      value={value}
      root={{ ref: wrapperRef }}
      style={{ flex: 1, minHeight: fieldHeight }}
      textarea={{
        style: {
          height: contentAreaHeight,
          minHeight: contentAreaMin,
          paddingTop: verticalPadding,
          paddingBottom: 0,
          minWidth: "50px",
          maxWidth: "100%",
          flex: 1,
        },
        ref: fieldRef,
      }}
    />
  );
}
