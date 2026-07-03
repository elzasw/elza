---
paths:
 - "**/*.tsx"
 - "**/*.jsx"
 - "**/*.ts"
---

# Typescript components

- Create new components as functional TypeScript components
- Define the props as a non-exported `interface Props` inside the component file
- Re-export it from the same file as `export type {ComponentName}Props = Props`
- Consumers import `{ComponentName}Props` directly (no aliasing needed)


## File & naming

- One component per file; filename matches the component name (`FilterText.tsx` exports `FilterText`).
- PascalCase for components and their files; camelCase for hooks (`useFoo.ts`) and utilities.
- Co-locate component-only helpers (sub-components, small hooks, pure functions) in the same file when they aren't reused. Promote to a sibling file once a second consumer appears.
- When there is a reason to promote a helper, component, or hook to a shared location (e.g. a second consumer appears in a different feature folder, or a clearly reusable primitive emerges), do not promote silently. Tell the user *what* should be promoted, *why*, and *where you propose to put it*, then ask whether to proceed. The shared directory layout in this codebase is in flux, so the destination is a judgment call the user should confirm.
- When suggesting a destination, prefer the modern, typed locations (`src/utils/`, `src/utils/hooks/`) over the legacy ones (`src/shared/`, `src/components/shared/`). If the right home for a shared UI primitive isn't obvious, say so — don't force a placement.

## Props

- Use `interface Props` (not `type`) — better error messages, extendable, plays well with `extends`.
- For DOM-wrapping components, extend the native element's props:
  `interface Props extends React.ComponentPropsWithoutRef<'button'> { … }`.
- Mark optional props with `?`, not `| undefined`. Don't default a prop in the type and again in destructuring.
- Don't use `React.FC` / `React.FunctionComponent` — declare the function signature directly. It gives better inference and avoids the implicit `children` prop.
- Default values go in destructuring, not in `defaultProps`:
  `function Foo({ size = 'medium' }: Props) { … }`.
- Keep prop lists short. If a component takes more than ~7 props or several booleans that toggle each other, consider splitting the component or grouping related props into an object.

## Component shape

- Prefer `function Foo(props: Props) { … }` over `const Foo = (props: Props) => { … }` — hoisting, named stack frames, and consistent with the rest of the codebase.
- Export the component as a named export, not default. Default exports lose the name on auto-import and make refactors noisier.
- Keep components pure: no side effects in the render body. All effects in `useEffect` / event handlers.
- Don't return `null` from a component to "hide" it — let the parent decide whether to render it. Exception: trivial guards (`if (!data) return null;`).
- When making non-trivial changes to a legacy class `.jsx` component, ask the user whether to migrate it to a functional `.tsx` component first.
  - Migrate → do the migration only, then stop and ask the user whether to proceed with the feature change on top, or wait for them to review/commit the migration first.
  - Skip migration → keep the change minimal and don't mix style cleanups in.
  - Don't decide silently.
- When changing a component that uses `redux-form`, ask the user whether to migrate it to `react-final-form` first. `redux-form` is unmaintained legacy.
  - Migrate → do the migration only, then stop and ask the user whether to proceed with the feature change on top, or wait for them to review/commit the migration first.
  - Skip migration → keep the change minimal.
  - Don't decide silently.

## State & effects

- Lift state only as far as it needs to go. Local state stays local.
- Derive, don't duplicate. If `fullName` can be computed from `firstName` + `lastName`, don't store it in state.
- One concern per `useEffect`. Multiple unrelated effects → multiple hooks.
- Always include the full dependency array. If you need to skip a dep, refactor (extract a ref, memoise, move logic out) — don't lie to the linter.
- Reach for `useMemo` / `useCallback` only when there's a measured cost or a referential-stability requirement (passing to `memo`'d children, effect deps). Don't blanket-memoise.

## Redux / data

- Use the typed `useAppSelector` from `utils/hooks` to read Redux state.
- Use the typed `useAppThunkDispatch` from `utils/hooks` to dispatch thunks.
- Two API clients exist; their endpoint coverage is mostly disjoint:
  - **New:** generated from the OpenAPI spec, exposed via the `Api` object in `src/api/api.ts` (`Api.funds`, `Api.accesspoints`, etc.). The underlying classes come from the `elza-api` package — do not edit them by hand.
  - **Legacy:** `WebApi` from `actions` (defined in `src/actions/WebApi.ts`).
- When implementing a call, check both clients. If the endpoint exists on `elza-api`, use it — even in code that already uses `WebApi` for other calls. Fall back to `WebApi` only when the new client doesn't cover the endpoint.
- Don't put server-shaped data in component state if it already lives in Redux. Read it from the store.

## Styling

- Use Fluent UI components (`@fluentui/react-components`) for new UI; React Bootstrap is legacy.
- Define styles with `makeStyles` from `@fluentui/react-components`. Call the hook inside the component (`const styles = useStyles();`) and apply with `className={styles.foo}`. Prefer this over SCSS for new components.
- SCSS is acceptable when extending existing SCSS-styled components or for global / theme-level styles that don't fit `makeStyles` (e.g. CSS variables, `:root` rules).
- When editing a component that already has SCSS, ask the user whether to keep using SCSS, migrate to `makeStyles`, or mix the two — don't decide silently.
- Inline `style` is acceptable for genuinely one-off layout. If the same inline style appears in more than one place, extract it into `makeStyles` instead.

## i18n

See `.claude/rules/i18n.md` for the full i18n rules (string definitions, build pipeline, corrector workflow).

## Accessibility

- Interactive elements must be real buttons / links / inputs, not `<div onClick>`. If a `div` has to be clickable, add `role`, `tabIndex`, and keyboard handlers.
- Label every form control (`<label htmlFor>` or `aria-label`).
- Don't remove focus outlines without providing an alternative focus indicator.

## Testing & types

- Avoid `any`. Prefer `unknown` + narrowing, or a precise type. The existing codebase has `as any` casts in legacy code — don't add new ones.
- Don't suppress with `// @ts-ignore` / `// @ts-expect-error` without a comment explaining why.
- No `console.log` in committed code. `console.warn` / `console.error` are acceptable for genuine warnings (the existing `onClose` fallbacks are an example).

## Comments

- Default to no comments. Add one only when the *why* is non-obvious (hidden constraint, subtle invariant, workaround).
- Comments describe current behaviour, not history. Don't write "previously this…" or "fix for #1234" — that goes in the commit message.
- Existing comments in this codebase are predominantly Czech. New comments in English. When editing a Czech comment, keep it Czech.


