import { Textarea, TextareaProps } from "@fluentui/react-components";
import { useLayoutEffect, useRef, useState } from "react";

interface Props extends TextareaProps {}

export function TextareaAutosize({ value, ...otherProps }: Props) {
  const [textAreaHeight, setTextAreaHeight] = useState("1em");

  const virtualFieldRef = useRef<HTMLDivElement>(null);
  const fieldRef = useRef<HTMLTextAreaElement>(null);
  const [padding, setPadding] = useState("4px 12px");
  const [minHeight, setMinHeight] = useState("28px");

  useLayoutEffect(() => {
    if (fieldRef.current) {
      const computed = getComputedStyle(fieldRef.current);
      setPadding(`${computed.paddingTop} ${computed.paddingRight} ${computed.paddingBottom} ${computed.paddingLeft}`);
      const lineHeight = parseFloat(computed.lineHeight) || parseFloat(computed.fontSize) * 1.2;
      const verticalPadding = parseFloat(computed.paddingTop) + parseFloat(computed.paddingBottom);
      setMinHeight(`${lineHeight + verticalPadding}px`);
    }
  }, [otherProps.size]);

  useLayoutEffect(() => {
    setTextAreaHeight(`${virtualFieldRef.current?.offsetHeight + 0}px`);
  }, [value, padding]);

  return (
    <>
      <div
        ref={virtualFieldRef}
        style={{
          visibility: "hidden",
          zIndex: -10000,
          position: "fixed",
          display: "inline-block",
          wordBreak: "break-word",
          padding,
          maxWidth: "100%",
          width: fieldRef.current?.offsetWidth,
        }}
      >
        {value}.
      </div>
      <Textarea
        {...otherProps}
        value={value}
        style={{ flex: 1 }}
        textarea={{
          style: {
            height: textAreaHeight,
            minHeight,
            minWidth: "50px",
            maxWidth: "100%",
            flex: 1,
          },
          ref: fieldRef,
        }}
      />
    </>
  );
}
