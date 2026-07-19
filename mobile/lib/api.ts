import { getAccessToken } from './auth0Client';

// Change this to your Mac's LAN IP (not "localhost") when testing on a physical phone —
// the phone is a separate device on your Wi-Fi and can't resolve "localhost" as your Mac.
// Find it with `ipconfig getifaddr en0` on your Mac. Keep the port matching
// server.port in application.yml (8090 by default).
export const API_BASE_URL = 'http://192.168.0.103:8090';

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
