import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Add01Icon, Remove01Icon } from '@hugeicons/core-free-icons';
import type {
  ScheduleEvent,
  ScheduleEventSetting,
  ScheduleStage,
  ScheduleStageStart,
} from '@/api/models';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { inputClass } from '@/components/ui/styles';
import { formatDayTime, formatTime, isoToTahitiInput, tahitiInputToIso } from '@/lib/dates';
import type { EventColor } from '../colors';

const MAX_OFFSET = 40;
const STAGES: ScheduleStage[] = ['KNOCKOUT', 'QUARTER', 'SEMI', 'FINAL'];

const toIso = (value: string) => (value ? tahitiInputToIso(value) : null);
const toInput = (iso: string | null | undefined) => (iso ? isoToTahitiInput(iso) : '');

const iconButton =
  'text-sable-600 hover:bg-sable-100 flex size-7 items-center justify-center rounded disabled:opacity-30';

export function EventOrderPanel({
  events,
  eventName,
  colors,
  averageMinutes,
  focusedId,
  pending,
  onFocus,
  onApply,
}: {
  events: ScheduleEvent[];
  eventName: (eventId: string) => string;
  colors: Map<string, EventColor>;
  averageMinutes: number;
  focusedId: string | null;
  pending: boolean;
  onFocus: (eventId: string | null) => void;
  onApply: (settings: ScheduleEventSetting[]) => void;
}) {
  const { t } = useTranslation();
  const initial: ScheduleEventSetting[] = events.map((e) => ({
    eventId: e.eventId,
    offsetSlots: e.offsetSlots,
    startAt: e.startAt ?? null,
    stageStarts: e.stageStarts ?? [],
  }));
  const [rows, setRows] = useState<ScheduleEventSetting[]>(initial);
  const [openId, setOpenId] = useState<string | null>(null);

  const update = (eventId: string, change: Partial<ScheduleEventSetting>) =>
    setRows((current) =>
      current.map((row) => (row.eventId === eventId ? { ...row, ...change } : row)),
    );

  const updateStage = (
    row: ScheduleEventSetting,
    index: number,
    change: Partial<ScheduleStageStart>,
  ) =>
    update(row.eventId, {
      stageStarts: (row.stageStarts ?? []).map((rule, i) =>
        i === index ? { ...rule, ...change } : rule,
      ),
    });
  const dirty = JSON.stringify(rows) !== JSON.stringify(initial);
  const info = new Map(events.map((e) => [e.eventId, e]));

  const swap = (index: number, other: number) =>
    setRows((current) => {
      const next = [...current];
      [next[index], next[other]] = [next[other], next[index]];
      return next;
    });

  const shift = (index: number, delta: number) =>
    setRows((current) =>
      current.map((row, i) =>
        i === index
          ? {
              ...row,
              offsetSlots: Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, row.offsetSlots + delta)),
            }
          : row,
      ),
    );

  return (
    <section className="space-y-3 rounded-lg bg-white p-4 shadow-sm">
      <div>
        <h3 className="text-sm font-semibold">{t('schedule.order.title')}</h3>
        <p className="text-sable-600 text-xs">{t('schedule.order.hint')}</p>
      </div>

      <ol className="space-y-1.5">
        {rows.map((row, index) => {
          const color = colors.get(row.eventId);
          const details = info.get(row.eventId);
          const focused = focusedId === row.eventId;
          return (
            <li
              key={row.eventId}
              className={`flex flex-wrap items-center gap-2 rounded-md border px-2 py-1.5 ${
                focused ? 'border-sable-400 bg-sable-50' : 'border-sable-200'
              }`}
            >
              <span className="text-sable-500 w-5 text-right text-xs tabular-nums">
                {index + 1}
              </span>
              <span
                className="size-3 shrink-0 rounded-full"
                style={{ backgroundColor: color?.strong }}
                aria-hidden
              />
              <button
                type="button"
                onClick={() => onFocus(focused ? null : row.eventId)}
                className="min-w-0 flex-1 text-left"
                aria-pressed={focused}
              >
                <span className="block truncate text-sm font-medium">{eventName(row.eventId)}</span>
                <span className="text-sable-600 block text-xs">
                  {t('schedule.order.matches', { count: details?.matchCount ?? 0 })}
                  {details?.firstStartAt &&
                    details.lastEndAt &&
                    ` · ${formatDayTime(details.firstStartAt)} → ${formatTime(details.lastEndAt)}`}
                </span>
              </button>

              <div className="flex items-center gap-0.5">
                <button
                  type="button"
                  className={iconButton}
                  disabled={index === 0}
                  onClick={() => swap(index, index - 1)}
                  aria-label={t('schedule.order.up')}
                >
                  ↑
                </button>
                <button
                  type="button"
                  className={iconButton}
                  disabled={index === rows.length - 1}
                  onClick={() => swap(index, index + 1)}
                  aria-label={t('schedule.order.down')}
                >
                  ↓
                </button>
              </div>

              <button
                type="button"
                onClick={() => setOpenId(openId === row.eventId ? null : row.eventId)}
                aria-expanded={openId === row.eventId}
                className={`text-xs hover:underline ${
                  row.startAt || (row.stageStarts ?? []).length > 0
                    ? 'text-lagon-700 font-medium'
                    : 'text-sable-600'
                }`}
              >
                {t('schedule.order.options')}
              </button>

              <div className="flex items-center gap-1">
                <button
                  type="button"
                  className={iconButton}
                  onClick={() => shift(index, -1)}
                  disabled={row.offsetSlots <= -MAX_OFFSET}
                  aria-label={t('schedule.order.earlier')}
                >
                  <Icon icon={Remove01Icon} size={14} />
                </button>
                <span className="w-28 text-center text-xs tabular-nums">
                  {row.offsetSlots === 0
                    ? t('schedule.order.noOffset')
                    : t('schedule.order.offset', {
                        count: row.offsetSlots,
                        sign: row.offsetSlots > 0 ? '+' : '',
                        minutes: row.offsetSlots * averageMinutes,
                      })}
                </span>
                <button
                  type="button"
                  className={iconButton}
                  onClick={() => shift(index, 1)}
                  disabled={row.offsetSlots >= MAX_OFFSET}
                  aria-label={t('schedule.order.later')}
                >
                  <Icon icon={Add01Icon} size={14} />
                </button>
              </div>

              {openId === row.eventId && (
                <div className="bg-sable-50 w-full space-y-3 rounded-md p-3">
                  <label className="block text-xs">
                    <span className="text-sable-800 font-medium">
                      {t('schedule.order.startAt')}
                    </span>
                    <span className="text-sable-600 block">{t('schedule.order.startAtHint')}</span>
                    <input
                      type="datetime-local"
                      value={toInput(row.startAt)}
                      onChange={(e) => update(row.eventId, { startAt: toIso(e.target.value) })}
                      className={`${inputClass} sm:w-60`}
                    />
                  </label>

                  <div className="space-y-2 text-xs">
                    <p className="text-sable-800 font-medium">{t('schedule.order.stagesTitle')}</p>
                    <p className="text-sable-600">{t('schedule.order.stagesHint')}</p>
                    {(row.stageStarts ?? []).map((rule, ruleIndex) => (
                      <div key={ruleIndex} className="flex flex-wrap items-center gap-2">
                        <select
                          value={rule.stage}
                          onChange={(e) =>
                            updateStage(row, ruleIndex, { stage: e.target.value as ScheduleStage })
                          }
                          className={`${inputClass} mt-0 w-auto`}
                          aria-label={t('schedule.order.stage')}
                        >
                          {STAGES.map((stage) => (
                            <option key={stage} value={stage}>
                              {t(`schedule.stages.${stage}`)}
                            </option>
                          ))}
                        </select>
                        <span>{t('schedule.order.notBefore')}</span>
                        <input
                          type="datetime-local"
                          value={toInput(rule.at)}
                          onChange={(e) =>
                            e.target.value &&
                            updateStage(row, ruleIndex, { at: tahitiInputToIso(e.target.value) })
                          }
                          className={`${inputClass} mt-0 w-auto`}
                        />
                        <button
                          type="button"
                          onClick={() =>
                            update(row.eventId, {
                              stageStarts: (row.stageStarts ?? []).filter(
                                (_, i) => i !== ruleIndex,
                              ),
                            })
                          }
                          className="text-red-700 hover:underline"
                        >
                          {t('schedule.order.removeStage')}
                        </button>
                      </div>
                    ))}
                    <button
                      type="button"
                      onClick={() =>
                        update(row.eventId, {
                          stageStarts: [
                            ...(row.stageStarts ?? []),
                            {
                              stage: 'SEMI',
                              at: row.startAt ?? details?.lastEndAt ?? new Date().toISOString(),
                            },
                          ],
                        })
                      }
                      className="text-lagon-700 hover:underline"
                    >
                      {t('schedule.order.addStage')}
                    </button>
                  </div>
                </div>
              )}
            </li>
          );
        })}
      </ol>

      {dirty && (
        <div className="flex flex-wrap items-center justify-end gap-2">
          <span className="text-sable-600 text-xs">{t('schedule.order.applyHint')}</span>
          <button
            type="button"
            onClick={() => setRows(initial)}
            className="text-sable-600 text-sm hover:underline"
          >
            {t('common.cancel')}
          </button>
          <Button type="button" disabled={pending} onClick={() => onApply(rows)}>
            {t('schedule.order.apply')}
          </Button>
        </div>
      )}
    </section>
  );
}
