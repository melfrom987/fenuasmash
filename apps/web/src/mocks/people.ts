import type { Club, Gender, Player } from '@/api/models';

export const clubs: Club[] = [
  {
    id: '40000000-0000-4000-8000-000000000001',
    name: 'Badminton Club de Papeete',
    city: 'Papeete',
    address: 'Salle omnisports, avenue du Prince Hinoi',
    email: 'contact@bc-papeete.example',
    phone: '40 50 60 70',
    logoUrl: null,
  },
  {
    id: '40000000-0000-4000-8000-000000000002',
    name: "Volant de Faa'a",
    city: "Faa'a",
    address: null,
    email: 'volant.faaa@example.com',
    phone: null,
    logoUrl: null,
  },
  {
    id: '40000000-0000-4000-8000-000000000003',
    name: 'Moorea Badminton',
    city: 'Moorea',
    address: null,
    email: null,
    phone: '87 11 22 33',
    logoUrl: null,
  },
  {
    id: '40000000-0000-4000-8000-000000000004',
    name: 'Taravao Smash',
    city: 'Taravao',
    address: null,
    email: null,
    phone: null,
    logoUrl: null,
  },
];

const unranked = () => ({ points: null, position: null });

const lastNames = [
  'Teriitahi',
  'Tehei',
  'Manutahi',
  'Chen',
  'Dupont',
  'Tauotaha',
  'Lefèvre',
  'Wong',
  'Tetuanui',
  'Martin',
  'Raapoto',
  'Hiro',
  'Bernard',
  'Teihotu',
  'Lucas',
];
const maleFirstNames = ['Teva', 'Hiro', 'Manoa', 'Kevin', 'Raiarii', 'Lucas', 'Tamatea', 'Thomas'];
const femaleFirstNames = ['Hinano', 'Vaimiti', 'Moana', 'Léa', 'Heiata', 'Emma', 'Tiare', 'Chloé'];

const pad = (n: number, length: number) => String(n).padStart(length, '0');

export const players: Player[] = Array.from({ length: 34 }, (_, i) => {
  const gender: Gender = i % 2 === 0 ? 'MALE' : 'FEMALE';
  const firstNames = gender === 'MALE' ? maleFirstNames : femaleFirstNames;
  const club = clubs[i % clubs.length];
  const hasLicense = i % 5 !== 0;

  return {
    id: `50000000-0000-4000-8000-${pad(i + 1, 12)}`,
    firstName: firstNames[(i * 3) % firstNames.length],
    lastName: lastNames[i % lastNames.length],
    gender,
    birthDate: `${1965 + ((i * 7) % 50)}-${pad((i % 12) + 1, 2)}-${pad((i % 27) + 1, 2)}`,
    club: { id: club.id, name: club.name, logoUrl: club.logoUrl },
    licenseNumber: hasLicense ? `PF${pad(10_000 + i * 37, 6)}` : null,
    email: i % 3 === 0 ? `joueur${i + 1}@example.com` : null,
    phone: i % 4 === 0 ? `87 ${pad(10 + i, 2)} ${pad(20 + i, 2)} ${pad(30 + i, 2)}` : null,
    rankings: {
      singles: i < 12 ? { points: 1200 - i * 80, position: Math.floor(i / 2) + 1 } : unranked(),
      doubles: i % 2 === 0 ? { points: 900 - i * 20, position: i / 2 + 1 } : unranked(),
      mixed: i % 2 === 1 ? { points: 800 - i * 20, position: (i + 1) / 2 } : unranked(),
    },
  };
});
