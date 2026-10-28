const TIME_ZONE = 'Pacific/Tahiti';

const dateFormatter = new Intl.DateTimeFormat('fr-FR', {
  timeZone: TIME_ZONE,
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});

const timeFormatter = new Intl.DateTimeFormat('fr-FR', {
  timeZone: TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
});

export const formatDate = (iso: string) => dateFormatter.format(new Date(iso));
export const formatTime = (iso: string) => timeFormatter.format(new Date(iso));
export const tahitiInputToIso = (value: string) => new Date(`${value}:00-10:00`).toISOString();
