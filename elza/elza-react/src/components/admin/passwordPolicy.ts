import { PasswordPolicy } from 'elza-api';

/** Character groups: lowercase letters, uppercase letters, digits, other characters. */
export const CHAR_GROUP_COUNT = 4;

export type PasswordPolicyViolation =
    | { rule: 'minLength'; minLength: number }
    | { rule: 'minCharGroups'; minCharGroups: number };

/** Number of character groups present in the password; mirrors the server-side check. */
export function countCharGroups(password: string): number {
    let lower = false;
    let upper = false;
    let digit = false;
    let other = false;
    for (const ch of password) {
        if (/\p{Ll}/u.test(ch)) {
            lower = true;
        } else if (/\p{Lu}/u.test(ch)) {
            upper = true;
        } else if (/\p{Nd}/u.test(ch)) {
            digit = true;
        } else {
            other = true;
        }
    }
    return Number(lower) + Number(upper) + Number(digit) + Number(other);
}

/** First rule of the policy the password violates, or null. Null or <= 0 turns a rule off. */
export function checkPasswordPolicy(password: string, policy?: PasswordPolicy | null): PasswordPolicyViolation | null {
    const minLength = policy?.minLength ?? 0;
    if (minLength > 0 && [...password].length < minLength) {
        return { rule: 'minLength', minLength };
    }
    const minCharGroups = Math.min(policy?.minCharGroups ?? 0, CHAR_GROUP_COUNT);
    if (minCharGroups > 0 && countCharGroups(password) < minCharGroups) {
        return { rule: 'minCharGroups', minCharGroups };
    }
    return null;
}
