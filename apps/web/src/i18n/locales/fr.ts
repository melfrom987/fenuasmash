export const fr = {
  common: {
    loading: 'Chargement…',
    language: 'Langue',
  },
  nav: {
    tournaments: 'Tournois',
  },
  live: {
    header: 'Live',
    placeholder: 'Les scores en direct apparaîtront ici.',
  },
  display: {
    placeholder: 'Les terrains apparaîtront ici.',
  },
  notFound: {
    title: 'Page introuvable',
    backToAdmin: "Retour à l'administration",
  },
  tournaments: {
    title: 'Tournois',
    new: 'Nouveau tournoi',
    manage: 'Gérer',
    backToList: '← Retour aux tournois',
    empty: 'Aucun tournoi pour le moment.',
    loadError: 'Impossible de charger les tournois : {{message}}',
    dateRange: 'Du {{start}} au {{end}}',
    liveLink: 'Page live',
    displayLink: 'Écran TV',
    create: {
      title: 'Nouveau tournoi',
      timezoneHint: 'Les horaires sont en heure de Tahiti.',
      organizationsError: 'Organisations : {{message}}',
    },
    form: {
      organization: 'Organisation',
      name: 'Nom du tournoi',
      namePlaceholder: 'Open de Papeete',
      startsAt: 'Début',
      endsAt: 'Fin',
      minimumRest: 'Repos minimum entre deux matchs (minutes)',
      submit: 'Créer le tournoi',
      submitting: 'Création…',
      courtCount: 'Nombre de terrains',
      errors: {
        courtCountRange: 'Entre 1 et 50 terrains',
        organizationRequired: 'Choisissez une organisation',
        nameRequired: 'Le nom est obligatoire',
        startsAtRequired: 'La date de début est obligatoire',
        endsAtRequired: 'La date de fin est obligatoire',
        numberExpected: 'Indiquez un nombre',
        integerExpected: 'Nombre entier attendu',
        positive: 'Doit être positif',
        endsBeforeStart: 'La fin doit être après le début',
      },
    },
  },
  tournamentDetail: {
    tabs: {
      settings: 'Paramètres',
    },
  },
  courts: {
    title: 'Terrains',
    hint: 'Les terrains sont numérotés automatiquement. Un terrain désactivé (panne, éclairage…) ne reçoit plus de matchs.',
    label: 'Terrain {{number}}',
    total: 'Nombre de terrains : {{total}}',
    add: 'Ajouter un terrain',
    removeLast: 'Retirer le terrain {{number}}',
    active: 'Actif',
    inactive: 'Désactivé',
    activate: 'Réactiver',
    deactivate: 'Désactiver',
  },
  apiErrors: {
    TOURNAMENT_NOT_FOUND: 'Tournoi introuvable',
    COURT_NOT_FOUND: 'Terrain introuvable',
    COURT_NOT_LAST: 'Seul le dernier terrain peut être retiré.',
    COURT_IN_USE: 'Ce terrain a déjà reçu des matchs : désactivez-le plutôt.',
  },
};

export type Translation = typeof fr;
