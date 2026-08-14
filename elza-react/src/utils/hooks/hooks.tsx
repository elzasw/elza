import {DependencyList, EffectCallback, useEffect, useLayoutEffect, useRef} from "react";
import {getOneSettings} from "components/arr/ArrUtils";
import {useAppSelector} from "utils/hooks/useAppSelector";

export const useThrottledEffect = (
    effect: EffectCallback,
    delay: number,
    deps: DependencyList = [],
    force?: boolean
) => {
    const lastRan = useRef(Date.now());

    useEffect(
        () => {
            if (!force) {
                const handler = setTimeout(function () {
                    if (Date.now() - lastRan.current >= delay) {
                        effect();
                        lastRan.current = Date.now();
                    }
                }, delay - (Date.now() - lastRan.current));

                return () => {
                    clearTimeout(handler);
                };
            } else {
                effect();
            }
        },
        [delay, ...deps],
    );
};

export const useDebouncedEffect = (
    effect: EffectCallback,
    delay: number,
    deps: DependencyList = [],
) => {
    useEffect(
        () => {
            const handler = setTimeout(effect, delay);
            return () => clearTimeout(handler);
        },
        [delay, ...deps],
    );
};

export function useStrictMode() {
  const strictMode: boolean = useAppSelector(({ userDetail, arrRegion }) => {
    const activeFund = arrRegion.funds[arrRegion.activeIndex];
    const strictModeSetting = getOneSettings(
      userDetail.settings,
      "FUND_STRICT_MODE",
      "FUND",
      activeFund.id,
    );
    const strictModeValue = strictModeSetting
      ? JSON.parse(strictModeSetting?.value)
      : true;
    return strictModeValue == null ? true : strictModeValue;
  });

  return strictMode;
}

export function useActiveFund() {
  const activeFund = useAppSelector(({ arrRegion }) =>
    arrRegion.activeIndex != undefined
      ? arrRegion.funds[arrRegion.activeIndex]
      : undefined,
  );
  return activeFund;
}

export function useActiveParent() {
  const activeFund = useActiveFund();
  const activeParent =
    activeFund.nodes.activeIndex != undefined
      ? activeFund.nodes.nodes[activeFund.nodes.activeIndex]
      : undefined;
  return activeParent;
}

export function useActiveNode() {
  const activeParent = useActiveParent();
  const activeNode = activeParent.childNodes.find(
    ({ id }) => id === activeParent.selectedSubNodeId,
  );
  return activeNode;
}

export const useDebouncedLayoutEffect = (
    effect: EffectCallback,
    delay: number,
    deps: DependencyList = [],
    force?: boolean
) => {
    useLayoutEffect(
        () => {
            if (force) {
                effect();
                return;
            }
            const handler = setTimeout(effect, delay);
            return () => clearTimeout(handler);
        },
        [delay, ...deps],
    );
};
