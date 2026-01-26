import { Textarea, TextareaProps } from "@fluentui/react-components";
import { useLayoutEffect, useRef, useState } from "react";

interface Props extends TextareaProps {}

export function TextareaAutosize({ value, ...otherProps }: Props) {
  const [textAreaHeight, setTextAreaHeight] = useState("1em");
  // const [textAreaWidth, setTextAreaWidth] = useState("50px");

  const virtualFieldRef = useRef<HTMLDivElement>(null);
  const fieldRef = useRef<HTMLTextAreaElement>(null);

  useLayoutEffect(() => {
    // console.log("#dit", "vf height", virtualFieldRef.current?.offsetHeight, value.length);
    setTextAreaHeight(`${virtualFieldRef.current?.offsetHeight + 0}px`);
    // setTextAreaWidth(`${virtualFieldRef.current?.offsetWidth + 4}px`);
  }, [value]);

  return (
    <>
      <div
        ref={virtualFieldRef}
        style={{
          // left: "-10000px",
          // left: "120vw",
          // left: 0,
          visibility: "hidden",
          zIndex: -10000,
          // top: 0,
          position: "fixed",
          display: "inline-block",
          // width: fieldRef.current?.offsetWidth,
          wordBreak: "break-word",
          padding: "4px 12px 4px 12px",
          // zIndex: 1000,
          background: "red",
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
            minHeight: "28px",
            // width: textAreaWidth,
            minWidth: "50px",
            maxWidth: "100%",
            paddingTop: "4px",
            paddingBottom: "4px",
            flex: 1,
          },
          ref: fieldRef,
        }}
      />
    </>
  );
}
