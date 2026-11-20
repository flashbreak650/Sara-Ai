export type AccentId = 'cyan' | 'violet' | 'emerald' | 'blue';
export type ThemeId = 'dark' | 'light' | 'system';
export type TextSizeId = 'small' | 'medium' | 'large';

export interface PreviewMemoryItem {
  id: string;
  content: string;
  createdAt: string;
}

export interface PreviewSettings {
  assistantName: string;
  personalityDescription: string;
  personalityMode: 'friendly' | 'professional';
  responseStyle: 'balanced' | 'concise' | 'empathetic' | 'analytical';
  answerLength: 'short' | 'detailed';

  voiceResponsesEnabled: boolean;
  voiceGender: 'female' | 'male' | 'system';
  speechSpeed: number;
  speechPitch: number;
  voiceLanguage: 'bn' | 'en';
  voiceVolume: number;

  wakeWordEnabled: boolean;
  customWakeWord: string;
  wakeListeningActive: boolean;

  selectedModel: 'gemini-4-hydrogen' | 'gemini-4-neon' | 'gemini-3.1-flash-lite';
  internetSearchEnabled: boolean;
  rememberContextEnabled: boolean;
  streamResponsesEnabled: boolean;
  apiProxyUrl: string;

  memoryEnabled: boolean;
  memoryPrivacyMask: boolean;
  memories: PreviewMemoryItem[];
  conversationCount: number;

  themeMode: ThemeId;
  accentColor: AccentId;
  animationsEnabled: boolean;
  textSize: TextSizeId;

  assistantNotificationsEnabled: boolean;
  voiceNotificationsEnabled: boolean;
  backgroundServiceNotificationEnabled: boolean;

  micPrivacyAutoMuteOnLock: boolean;
  screenCaptureAlwaysConfirm: boolean;
  appLockEnabled: boolean;
}

export const ACCENT_TOKENS: Record<
  AccentId,
  { labelBn: string; hex: string; rgb: string; secondaryRgb: string }
> = {
  cyan: { labelBn: 'সায়ান', hex: '#22D3EE', rgb: '34, 211, 238', secondaryRgb: '139, 92, 246' },
  violet: { labelBn: 'ভায়োলেট', hex: '#8B5CF6', rgb: '139, 92, 246', secondaryRgb: '34, 211, 238' },
  emerald: { labelBn: 'এমেরাল্ড', hex: '#10B981', rgb: '16, 185, 129', secondaryRgb: '34, 211, 238' },
  blue: { labelBn: 'ব্লু', hex: '#38BDF8', rgb: '56, 189, 248', secondaryRgb: '167, 139, 250' },
};

export const DEFAULT_PREVIEW_SETTINGS: PreviewSettings = {
  assistantName: 'SARA',
  personalityDescription: 'Helpful, calm, and intelligent Bengali-first assistant',
  personalityMode: 'friendly',
  responseStyle: 'balanced',
  answerLength: 'short',

  voiceResponsesEnabled: true,
  voiceGender: 'female',
  speechSpeed: 1.0,
  speechPitch: 1.0,
  voiceLanguage: 'bn',
  voiceVolume: 0.85,

  wakeWordEnabled: false,
  customWakeWord: 'Hey SARA',
  wakeListeningActive: false,

  selectedModel: 'gemini-4-hydrogen',
  internetSearchEnabled: true,
  rememberContextEnabled: true,
  streamResponsesEnabled: true,
  apiProxyUrl: '',

  memoryEnabled: true,
  memoryPrivacyMask: false,
  memories: [],
  conversationCount: 0,

  themeMode: 'dark',
  accentColor: 'cyan',
  animationsEnabled: true,
  textSize: 'medium',

  assistantNotificationsEnabled: true,
  voiceNotificationsEnabled: true,
  backgroundServiceNotificationEnabled: true,

  micPrivacyAutoMuteOnLock: true,
  screenCaptureAlwaysConfirm: true,
  appLockEnabled: false,
};

const STORAGE_KEY = 'sara_preview_settings_v2';

export function loadPreviewSettings(): PreviewSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_PREVIEW_SETTINGS;
    return { ...DEFAULT_PREVIEW_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_PREVIEW_SETTINGS;
  }
}

export function savePreviewSettings(settings: PreviewSettings): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch {
    // Ignore storage quota errors in restricted frames
  }
}
