import { useState } from 'react';
import {
  ArrowLeft,
  Bell,
  Cpu,
  Database,
  Info,
  Mic,
  Palette,
  Shield,
  Smartphone,
  Sparkles,
  Trash2,
  Volume2,
} from 'lucide-react';
import {
  ACCENT_TOKENS,
  type AccentId,
  type PreviewSettings,
  type TextSizeId,
  type ThemeId,
} from '../data/previewSettings';

interface SettingsPhoneViewProps {
  settings: PreviewSettings;
  onUpdate: (updater: (prev: PreviewSettings) => PreviewSettings) => void;
  onBack: () => void;
  isDark: boolean;
}

export function SettingsPhoneView({
  settings,
  onUpdate,
  onBack,
  isDark,
}: SettingsPhoneViewProps) {
  const [voiceNotice, setVoiceNotice] = useState<string | null>(null);
  const [wakeNotice, setWakeNotice] = useState<string | null>(null);
  const [newMemoryText, setNewMemoryText] = useState('');
  const [infoModal, setInfoModal] = useState<'privacy' | 'help' | null>(null);

  const accent = ACCENT_TOKENS[settings.accentColor];
  const surfaceBg = isDark ? 'bg-[#0D1529] border-[#1E2B4D]' : 'bg-white border-slate-200';
  const innerBg = isDark ? 'bg-[#060B18] border-[#1E2B4D]' : 'bg-slate-50 border-slate-200';
  const textMain = isDark ? 'text-slate-100' : 'text-slate-900';
  const textSub = isDark ? 'text-slate-400' : 'text-slate-600';

  const handleTestVoice = () => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      setVoiceNotice('SpeechSynthesis unavailable in this browser; works natively via Android TTS in the APK.');
      return;
    }
    window.speechSynthesis.cancel();
    const text =
      settings.voiceLanguage === 'bn'
        ? `নমস্কার, আমি ${settings.assistantName || 'SARA'}। আপনার ভয়েস সেটিংস সক্রিয় রয়েছে।`
        : `Hello, I am ${settings.assistantName || 'SARA'}. Your voice settings are active.`;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = settings.voiceLanguage === 'bn' ? 'bn-BD' : 'en-US';
    utterance.rate = settings.speechSpeed;
    utterance.pitch = settings.speechPitch;
    utterance.volume = settings.voiceVolume;
    window.speechSynthesis.speak(utterance);
    setVoiceNotice(`Speaking (${settings.voiceLanguage === 'bn' ? 'Bengali' : 'English'})`);
  };

  const handleAddMemory = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = newMemoryText.trim();
    if (!clean || !settings.memoryEnabled) return;
    const item = {
      id: `${Date.now()}`,
      content: clean,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    onUpdate((prev) => ({ ...prev, memories: [item, ...prev.memories] }));
    setNewMemoryText('');
  };

  return (
    <div className="flex h-[670px] flex-col">
      {/* Top Bar */}
      <div
        className={`flex items-center gap-2 border-b px-3 py-2.5 ${
          isDark ? 'border-[#1E2B4D] bg-[#0D1529]' : 'border-slate-200 bg-white'
        }`}
      >
        <button
          type="button"
          onClick={onBack}
          aria-label="Back to Home"
          className="flex h-10 w-10 items-center justify-center rounded-xl transition-colors cursor-pointer"
          style={{ color: accent.hex }}
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div className="min-w-0 flex-1">
          <div className={`text-sm font-semibold ${textMain}`} style={{ fontFamily: 'var(--font-bengali)' }}>
            সারা সেটিংস
          </div>
          <div className={`text-[11px] ${textSub}`}>SARA Control Center · 10 Modules</div>
        </div>
      </div>

      {/* Scrollable 10 Categories */}
      <div className="flex-1 space-y-3 overflow-y-auto p-3 text-xs">
        {/* 1. AI Personality */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2.5 flex items-center gap-2">
            <Sparkles className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>১. এআই ব্যক্তিত্ব</div>
              <div className={`text-[11px] ${textSub}`}>AI Personality</div>
            </div>
          </div>

          <label className={`block text-[11px] ${textSub}`}>Assistant Name</label>
          <input
            type="text"
            value={settings.assistantName}
            onChange={(e) => onUpdate((p) => ({ ...p, assistantName: e.target.value }))}
            className={`mt-1 w-full rounded-lg border px-2.5 py-1.5 text-xs ${innerBg} ${textMain}`}
          />

          <label className={`mt-2.5 block text-[11px] ${textSub}`}>Personality Prompt</label>
          <input
            type="text"
            value={settings.personalityDescription}
            onChange={(e) => onUpdate((p) => ({ ...p, personalityDescription: e.target.value }))}
            className={`mt-1 w-full rounded-lg border px-2.5 py-1.5 text-xs ${innerBg} ${textMain}`}
          />

          <div className="mt-2.5 grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => onUpdate((p) => ({ ...p, personalityMode: 'friendly' }))}
              className={`min-h-[34px] rounded-lg border px-2 font-medium cursor-pointer ${
                settings.personalityMode === 'friendly' ? 'font-semibold' : textSub
              }`}
              style={
                settings.personalityMode === 'friendly'
                  ? { borderColor: accent.hex, color: accent.hex }
                  : undefined
              }
            >
              বন্ধুত্বপূর্ণ · Friendly
            </button>
            <button
              type="button"
              onClick={() => onUpdate((p) => ({ ...p, personalityMode: 'professional' }))}
              className={`min-h-[34px] rounded-lg border px-2 font-medium cursor-pointer ${
                settings.personalityMode === 'professional' ? 'font-semibold' : textSub
              }`}
              style={
                settings.personalityMode === 'professional'
                  ? { borderColor: accent.hex, color: accent.hex }
                  : undefined
              }
            >
              পেশাদার · Pro
            </button>
          </div>

          <div className="mt-2.5 flex items-center justify-between gap-2">
            <span className={textSub}>Response Style</span>
            <select
              value={settings.responseStyle}
              onChange={(e) =>
                onUpdate((p) => ({
                  ...p,
                  responseStyle: e.target.value as PreviewSettings['responseStyle'],
                }))
              }
              className={`rounded-lg border px-2 py-1 text-xs ${innerBg} ${textMain}`}
            >
              <option value="balanced">Balanced</option>
              <option value="concise">Direct & Concise</option>
              <option value="empathetic">Empathetic</option>
              <option value="analytical">Analytical</option>
            </select>
          </div>

          <div className="mt-2 grid grid-cols-2 gap-2">
            {(['short', 'detailed'] as const).map((len) => (
              <button
                key={len}
                type="button"
                onClick={() => onUpdate((p) => ({ ...p, answerLength: len }))}
                className={`min-h-[32px] rounded-lg border text-[11px] cursor-pointer ${
                  settings.answerLength === len ? 'font-semibold' : textSub
                }`}
                style={
                  settings.answerLength === len
                    ? { borderColor: accent.hex, color: accent.hex }
                    : undefined
                }
              >
                {len === 'short' ? 'সংক্ষিপ্ত · Short' : 'বিস্তারিত · Detailed'}
              </button>
            ))}
          </div>
        </section>

        {/* 2. Voice Settings */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2.5 flex items-center gap-2">
            <Volume2 className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>২. ভয়েস সেটিংস</div>
              <div className={`text-[11px] ${textSub}`}>Voice & TTS Controls</div>
            </div>
          </div>

          <ToggleRow
            label="ভয়েস উত্তর চালু · Voice Responses"
            checked={settings.voiceResponsesEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, voiceResponsesEnabled: v }))}
          />

          <div className="mt-2 flex items-center justify-between gap-2">
            <span className={textSub}>Language / Voice</span>
            <div className="flex gap-1.5">
              <button
                type="button"
                onClick={() => onUpdate((p) => ({ ...p, voiceLanguage: 'bn' }))}
                className={`rounded-md border px-2 py-1 cursor-pointer ${
                  settings.voiceLanguage === 'bn' ? 'font-semibold' : textSub
                }`}
                style={settings.voiceLanguage === 'bn' ? { borderColor: accent.hex, color: accent.hex } : undefined}
              >
                বাংলা
              </button>
              <button
                type="button"
                onClick={() => onUpdate((p) => ({ ...p, voiceLanguage: 'en' }))}
                className={`rounded-md border px-2 py-1 cursor-pointer ${
                  settings.voiceLanguage === 'en' ? 'font-semibold' : textSub
                }`}
                style={settings.voiceLanguage === 'en' ? { borderColor: accent.hex, color: accent.hex } : undefined}
              >
                EN
              </button>
            </div>
          </div>

          <div className="mt-2 flex items-center justify-between gap-2">
            <span className={textSub}>Voice Gender</span>
            <select
              value={settings.voiceGender}
              onChange={(e) =>
                onUpdate((p) => ({
                  ...p,
                  voiceGender: e.target.value as PreviewSettings['voiceGender'],
                }))
              }
              className={`rounded-lg border px-2 py-1 text-xs ${innerBg} ${textMain}`}
            >
              <option value="female">Female (When available)</option>
              <option value="male">Male (When available)</option>
              <option value="system">System Default</option>
            </select>
          </div>

          <SliderRow
            label="Speech Speed"
            valueText={`${settings.speechSpeed.toFixed(2)}x`}
            value={settings.speechSpeed}
            min={0.5}
            max={2.0}
            step={0.05}
            accentHex={accent.hex}
            textSub={textSub}
            onChange={(v) => onUpdate((p) => ({ ...p, speechSpeed: v }))}
          />
          <SliderRow
            label="Speech Pitch"
            valueText={`${settings.speechPitch.toFixed(2)}x`}
            value={settings.speechPitch}
            min={0.5}
            max={2.0}
            step={0.05}
            accentHex={accent.hex}
            textSub={textSub}
            onChange={(v) => onUpdate((p) => ({ ...p, speechPitch: v }))}
          />
          <SliderRow
            label="Voice Volume"
            valueText={`${Math.round(settings.voiceVolume * 100)}%`}
            value={settings.voiceVolume}
            min={0}
            max={1}
            step={0.05}
            accentHex={accent.hex}
            textSub={textSub}
            onChange={(v) => onUpdate((p) => ({ ...p, voiceVolume: v }))}
          />

          <button
            type="button"
            onClick={handleTestVoice}
            className="mt-2.5 w-full min-h-[36px] rounded-xl font-semibold text-[#060B18] cursor-pointer"
            style={{ backgroundColor: accent.hex }}
          >
            ভয়েস পরীক্ষা করুন · Test Voice
          </button>
          {voiceNotice && <div className="mt-1.5 text-[11px]" style={{ color: accent.hex }}>{voiceNotice}</div>}
        </section>

        {/* 3. Wake Word Settings */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Mic className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৩. ওয়েক ওয়ার্ড</div>
              <div className={`text-[11px] ${textSub}`}>Hey SARA & Background Service</div>
            </div>
          </div>

          <ToggleRow
            label='"Hey SARA" Wake Word'
            checked={settings.wakeWordEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, wakeWordEnabled: v }))}
          />
          <input
            type="text"
            value={settings.customWakeWord}
            onChange={(e) => onUpdate((p) => ({ ...p, customWakeWord: e.target.value }))}
            aria-label="Custom wake word"
            className={`mt-1.5 w-full rounded-lg border px-2.5 py-1.5 text-xs ${innerBg} ${textMain}`}
          />
          <div className={`mt-2 space-y-1 text-[11px] ${textSub}`}>
            <div>Mic Permission: Checked natively via Android `RECORD_AUDIO`</div>
            <div>Battery Guidance: Configure Unrestricted mode in Android Settings</div>
          </div>
          <button
            type="button"
            onClick={() =>
              setWakeNotice(
                'Background foreground listening service requires native Android RECORD_AUDIO permission in the installed APK.'
              )
            }
            className={`mt-2 w-full min-h-[34px] rounded-lg border font-medium cursor-pointer ${innerBg}`}
            style={{ color: accent.hex }}
          >
            Check Listening Status
          </button>
          {wakeNotice && <div className={`mt-1.5 text-[11px] ${textSub}`}>{wakeNotice}</div>}
        </section>

        {/* 4. Phone Control Settings */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Smartphone className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৪. ফোন কন্ট্রোল</div>
              <div className={`text-[11px] ${textSub}`}>Native Android Permissions</div>
            </div>
          </div>
          <div className={`space-y-1.5 text-[11px] ${textSub}`}>
            <div className="flex justify-between">
              <span>App Launching</span>
              <span className="text-emerald-400">Launcher Query Ready</span>
            </div>
            <div className="flex justify-between">
              <span>Phone Call / SMS</span>
              <span className="text-amber-400">Android Runtime Prompt</span>
            </div>
            <div className="flex justify-between">
              <span>Accessibility & Notifications</span>
              <span className="text-amber-400">OS Settings Intent</span>
            </div>
            <div className="flex justify-between">
              <span>Screen Capture (MediaProjection)</span>
              <span className="text-emerald-400">Per-Session Consent</span>
            </div>
          </div>
        </section>

        {/* 5. AI Brain Settings (No raw API key input in web preview) */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Cpu className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৫. এআই ব্রেইন</div>
              <div className={`text-[11px] ${textSub}`}>Gemini Model & Preferences</div>
            </div>
          </div>
          <div className={`mb-2 text-[11px] ${textSub}`}>
            API Key Protection: Stored securely in Android Keystore (AES-256-GCM) in the APK.
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className={textSub}>Model</span>
            <select
              value={settings.selectedModel}
              onChange={(e) =>
                onUpdate((p) => ({
                  ...p,
                  selectedModel: e.target.value as PreviewSettings['selectedModel'],
                }))
              }
              className={`rounded-lg border px-2 py-1 text-xs ${innerBg} ${textMain}`}
            >
              <option value="gemini-4-hydrogen">Gemini 4 Hydrogen</option>
              <option value="gemini-4-neon">Gemini 4 Neon</option>
              <option value="gemini-3.1-flash-lite">Gemini 3.1 Flash Lite</option>
            </select>
          </div>
          <ToggleRow
            label="Internet Search Grounding"
            checked={settings.internetSearchEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, internetSearchEnabled: v }))}
          />
          <ToggleRow
            label="Remember Context"
            checked={settings.rememberContextEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, rememberContextEnabled: v }))}
          />
          <ToggleRow
            label="Stream Responses"
            checked={settings.streamResponsesEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, streamResponsesEnabled: v }))}
          />
        </section>

        {/* 6. Memory Settings */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Database className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৬. মেমোরি সেটিংস</div>
              <div className={`text-[11px] ${textSub}`}>Memories ({settings.memories.length}) & History</div>
            </div>
          </div>
          <ToggleRow
            label="Memory Enabled"
            checked={settings.memoryEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, memoryEnabled: v }))}
          />
          <ToggleRow
            label="Mask Memory Text"
            checked={settings.memoryPrivacyMask}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, memoryPrivacyMask: v }))}
          />

          <form onSubmit={handleAddMemory} className="mt-2 flex gap-1.5">
            <input
              type="text"
              value={newMemoryText}
              onChange={(e) => setNewMemoryText(e.target.value)}
              placeholder="Add memory note..."
              disabled={!settings.memoryEnabled}
              className={`min-w-0 flex-1 rounded-lg border px-2.5 py-1.5 text-xs ${innerBg} ${textMain}`}
            />
            <button
              type="submit"
              disabled={!settings.memoryEnabled}
              className="rounded-lg px-2.5 font-semibold text-[#060B18] cursor-pointer disabled:opacity-40"
              style={{ backgroundColor: accent.hex }}
            >
              Add
            </button>
          </form>

          {settings.memories.length > 0 && (
            <div className="mt-2 space-y-1.5">
              {settings.memories.map((m) => (
                <div
                  key={m.id}
                  className={`flex items-center justify-between rounded-lg border px-2.5 py-1.5 ${innerBg}`}
                >
                  <span className={`truncate pr-2 ${textMain}`}>
                    {settings.memoryPrivacyMask ? '••••••••••••' : m.content}
                  </span>
                  <button
                    type="button"
                    aria-label="Delete memory"
                    onClick={() =>
                      onUpdate((p) => ({
                        ...p,
                        memories: p.memories.filter((item) => item.id !== m.id),
                      }))
                    }
                    className="text-red-400 cursor-pointer"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}

          <button
            type="button"
            onClick={() => onUpdate((p) => ({ ...p, conversationCount: 0 }))}
            className={`mt-2 w-full min-h-[32px] rounded-lg border text-red-400 cursor-pointer ${innerBg}`}
          >
            Clear Conversation History ({settings.conversationCount})
          </button>
        </section>

        {/* 7. Appearance */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Palette className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৭. অ্যাপিয়ারেন্স</div>
              <div className={`text-[11px] ${textSub}`}>Theme, Accent, Animations & Text Size</div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-1.5">
            {(['dark', 'light', 'system'] as ThemeId[]).map((mode) => (
              <button
                key={mode}
                type="button"
                onClick={() => onUpdate((p) => ({ ...p, themeMode: mode }))}
                className={`min-h-[32px] rounded-lg border capitalize cursor-pointer ${
                  settings.themeMode === mode ? 'font-semibold' : textSub
                }`}
                style={settings.themeMode === mode ? { borderColor: accent.hex, color: accent.hex } : undefined}
              >
                {mode}
              </button>
            ))}
          </div>

          <div className="mt-2.5 grid grid-cols-4 gap-1.5">
            {(Object.keys(ACCENT_TOKENS) as AccentId[]).map((id) => {
              const token = ACCENT_TOKENS[id];
              const active = settings.accentColor === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onUpdate((p) => ({ ...p, accentColor: id }))}
                  className={`flex flex-col items-center gap-1 rounded-lg border py-1.5 cursor-pointer ${
                    active ? 'font-semibold' : textSub
                  }`}
                  style={active ? { borderColor: token.hex, color: token.hex } : undefined}
                >
                  <span className="h-3.5 w-3.5 rounded-full" style={{ backgroundColor: token.hex }} />
                  <span className="text-[10px]">{token.labelBn}</span>
                </button>
              );
            })}
          </div>

          <ToggleRow
            label="Animations Enabled"
            checked={settings.animationsEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, animationsEnabled: v }))}
          />

          <div className="mt-1 grid grid-cols-3 gap-1.5">
            {(['small', 'medium', 'large'] as TextSizeId[]).map((sz) => (
              <button
                key={sz}
                type="button"
                onClick={() => onUpdate((p) => ({ ...p, textSize: sz }))}
                className={`min-h-[30px] rounded-lg border capitalize cursor-pointer ${
                  settings.textSize === sz ? 'font-semibold' : textSub
                }`}
                style={settings.textSize === sz ? { borderColor: accent.hex, color: accent.hex } : undefined}
              >
                {sz}
              </button>
            ))}
          </div>
        </section>

        {/* 8. Notifications */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Bell className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৮. নোটিফিকেশন</div>
              <div className={`text-[11px] ${textSub}`}>Notification Controls</div>
            </div>
          </div>
          <ToggleRow
            label="Assistant Notifications"
            checked={settings.assistantNotificationsEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, assistantNotificationsEnabled: v }))}
          />
          <ToggleRow
            label="Voice Notification Alerts"
            checked={settings.voiceNotificationsEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, voiceNotificationsEnabled: v }))}
          />
          <ToggleRow
            label="Background Service Alert"
            checked={settings.backgroundServiceNotificationEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, backgroundServiceNotificationEnabled: v }))}
          />
        </section>

        {/* 9. Privacy & Security */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Shield className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>৯. প্রাইভেসি ও সিকিউরিটি</div>
              <div className={`text-[11px] ${textSub}`}>Privacy, App Lock & Data</div>
            </div>
          </div>
          <ToggleRow
            label="Auto-Mute Mic on Lock"
            checked={settings.micPrivacyAutoMuteOnLock}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, micPrivacyAutoMuteOnLock: v }))}
          />
          <ToggleRow
            label="Confirm Screen Capture"
            checked={settings.screenCaptureAlwaysConfirm}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, screenCaptureAlwaysConfirm: v }))}
          />
          <ToggleRow
            label="App Lock (Device Security)"
            checked={settings.appLockEnabled}
            accentHex={accent.hex}
            textMain={textMain}
            onChange={(v) => onUpdate((p) => ({ ...p, appLockEnabled: v }))}
          />
          <button
            type="button"
            onClick={() =>
              onUpdate((p) => ({
                ...p,
                assistantName: 'SARA',
                customWakeWord: 'Hey SARA',
                memories: [],
                conversationCount: 0,
              }))
            }
            className={`mt-2 w-full min-h-[34px] rounded-lg border text-red-400 cursor-pointer ${innerBg}`}
          >
            Clear Personal Data
          </button>
        </section>

        {/* 10. About SARA */}
        <section className={`rounded-2xl border p-3.5 ${surfaceBg}`}>
          <div className="mb-2 flex items-center gap-2">
            <Info className="h-4 w-4" style={{ color: accent.hex }} />
            <div>
              <div className={`font-semibold ${textMain}`}>১০. সারা সম্পর্কে</div>
              <div className={`text-[11px] ${textSub}`}>About SARA</div>
            </div>
          </div>
          <div className={`space-y-1 text-[11px] ${textSub}`}>
            <div className={`font-semibold ${textMain}`}>{settings.assistantName || 'SARA'} — Personal AI Assistant</div>
            <div>Version 0.2.0-settings (Build 2)</div>
            <div>Developer: Flash Break · Native Kotlin + Compose</div>
          </div>
          <div className="mt-2.5 grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setInfoModal('privacy')}
              className={`min-h-[32px] rounded-lg border cursor-pointer ${innerBg} ${textMain}`}
            >
              Privacy Policy
            </button>
            <button
              type="button"
              onClick={() => setInfoModal('help')}
              className={`min-h-[32px] rounded-lg border cursor-pointer ${innerBg} ${textMain}`}
            >
              Help & Support
            </button>
          </div>
          {infoModal && (
            <div className={`mt-2 rounded-lg border p-2.5 text-[11px] ${innerBg} ${textSub}`}>
              {infoModal === 'privacy'
                ? 'SARA encrypts API keys in Android Keystore (AES-256-GCM) and never collects or transmits mic/screen data without explicit user consent.'
                : 'Use Voice Settings to test speech, Wake Word to grant Microphone access, and Phone Control to manage Android permissions.'}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function ToggleRow({
  label,
  checked,
  accentHex,
  textMain,
  onChange,
}: {
  label: string;
  checked: boolean;
  accentHex: string;
  textMain: string;
  onChange: (val: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between py-1.5">
      <span className={textMain}>{label}</span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className="relative h-6 w-10 rounded-full transition-colors cursor-pointer"
        style={{ backgroundColor: checked ? accentHex : '#334155' }}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-transform ${
            checked ? 'left-4' : 'left-0.5'
          }`}
        />
      </button>
    </div>
  );
}

function SliderRow({
  label,
  valueText,
  value,
  min,
  max,
  step,
  accentHex,
  textSub,
  onChange,
}: {
  label: string;
  valueText: string;
  value: number;
  min: number;
  max: number;
  step: number;
  accentHex: string;
  textSub: string;
  onChange: (val: number) => void;
}) {
  return (
    <div className="mt-2">
      <div className="flex justify-between text-[11px]">
        <span className={textSub}>{label}</span>
        <span className="font-mono tabular-nums" style={{ color: accentHex }}>
          {valueText}
        </span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="w-full cursor-pointer"
        style={{ accentColor: accentHex }}
      />
    </div>
  );
}
