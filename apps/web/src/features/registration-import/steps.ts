export const STEPS = ['file', 'columns', 'values', 'preview', 'done'] as const;
export type Step = (typeof STEPS)[number];
