import { getAccessToken } from './auth0Client';

const _apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;
if (!_apiBaseUrl) {
  throw new Error(
    'EXPO_PUBLIC_API_BASE_URL is not set. ' +
    'Copy mobile/.env.example to mobile/.env and set EXPO_PUBLIC_API_BASE_URL ' +
    "to your Mac's LAN IP and backend port (e.g. http://192.168.1.x:8090). " +
    'Physical phones cannot reach "localhost" — they need the actual LAN IP.',
  );
}
export const API_BASE_URL: string = _apiBaseUrl;

async function authorizedFetch(path: string, options: RequestInit = {}): Promise<Response> {
  // getAccessToken() (see auth0Client.ts) transparently refreshes an expired token
  // using the stored refresh token — no manual token management here.
  const token = await getAccessToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.headers ?? {}),
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(`${path} -> ${response.status}: ${body}`);
  }
  return response;
}

export interface FacultySummary {
  id: string;
  displayName: string;
  role: string;
  practiceOrder: number;
}

export interface FacultyContent {
  id: string;
  displayName: string;
  role: string;
  treePosition: string;
  practiceOrder: number;
  teachingNotes: string[];
  hekau: { id: string; text: string }[];
  meditationInstructions: {
    breathingTechnique: { summary: string; inbreath: string; outbreath: string; principle: string };
    environmentProtocols: string[];
    expectedSensations: string;
    scriptSteps: { step: number; name: string; instruction: string }[];
    durationGuidance: string;
  };
  journalPrompts: string[];
  suggestedDailyActions: string[];
}

export async function fetchFacultyList(): Promise<FacultySummary[]> {
  const res = await authorizedFetch('/api/faculty');
  return res.json();
}

export async function fetchFaculty(id: string): Promise<FacultyContent> {
  const res = await authorizedFetch(`/api/faculty/${id}`);
  return res.json();
}

export interface UserProfile {
  id: string;
  displayName: string;
  activeFacultyId: string;
}

export async function fetchUserProfile(): Promise<UserProfile> {
  const res = await authorizedFetch('/api/user/me');
  return res.json();
}

export async function setActiveFaculty(facultyId: string): Promise<UserProfile> {
  const res = await authorizedFetch('/api/user/active-faculty', {
    method: 'PATCH',
    body: JSON.stringify({ facultyId }),
  });
  return res.json();
}

export async function markPracticeComplete(): Promise<{ completedDays: number }> {
  const res = await authorizedFetch('/api/practice/complete', { method: 'POST' });
  return res.json();
}

export interface ChatResponse {
  reply: string;
  aiGenerated: boolean;
}

export async function sendCompanionMessage(message: string): Promise<ChatResponse> {
  const res = await authorizedFetch('/api/companion/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  });
  return res.json();
}
