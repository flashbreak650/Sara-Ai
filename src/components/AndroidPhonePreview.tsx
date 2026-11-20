import { useEffect, useMemo, useState } from 'react';
import { Mic, Send, Settings } from 'lucide-react';
import {
  ACCENT_TOKENS,
  loadPreviewSettings,
  savePreviewSettings,
  type PreviewSettings,
} from '../data/previewSettings';
import { SaraOrbCanvas, type OrbVisualState } from './SaraOrbCanvas';
import { SettingsPhoneView } from './SettingsPhoneView';

type AppLanguage = 'bn' | 'en';

function getTimeGreeting(lang: AppLanguage): string {
  const hour = new Date().getHours();
  if (lang === 'bn') {
    if (hour >= 5 && hour <= 11) return 'শুভ সকাল';
    if (hour >= 12 && hour <= 15) return 'শুভ দুপুর';
    if (hour >= 16 && hour <= 18) return 'শুভ বিকাল';
    if (hour >= 19 && hour <= 21) return 'শুভ সন্ধ্যা';
    return 'শুভ রাত্রি';
  }
  if (hour >= 5 && hour <= 11) return 'Good Morning';
  if (hour >= 12 && hour <= 16) return 'Good Afternoon';
  if (hour >= 17 && hour <= 20) return 'Good Evening';
  return 'Good Night';
}

export function AndroidPhonePreview() {
  const [settings, setSettings] = useState<PreviewSettings>(() => loadPreviewSettings());
  const [screen, setScreen] = useState<'home' | 'settings'>('home');
  const [language, setLanguage] = useState<AppLanguage>(() => settings.voiceLanguage);
  const [orbState, setOrbState] = useState<OrbVisualState>('IDLE');
  const [draft, setDraft] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    savePreviewSettings(settings);
  }, [settings]);

  const updateSettings = (updater: (prev: PreviewSettings) => PreviewSettings) => {
    setSettings((prev) => updater(prev));
  };

  const isDark =
    settings.themeMode === 'dark' ||
    (settings.themeMode === 'system' &&
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches);

  const accent = ACCENT_TOKENS[settings.accentColor];
  const assistantName = settings.assistantName.trim() || 'SARA';
  const greeting = useMemo(() => getTimeGreeting(language), [language]);

  const textScaleClass =
    settings.textSize === 'small'
      ? 'text-[13px]'
      : settings.textSize === 'large'
        ? 'text-[16px]'
        : 'text-[14px]';

  const handleOrbTap = () => {
    setOrbState((prev) => {
      if (prev === 'IDLE') return 'ACTIVE_GREETING';
      if (prev === 'ACTIVE_GREETING') return 'LISTENING_READY';
      return 'IDLE';
    });
    const msg =
      language === 'bn'
        ? `${assistantName} প্রস্তুত। ভয়েস, মেমোরি ও সেটিংস সক্রিয় রয়েছে।`
        : `${assistantName} is ready. Voice, memory & settings are active.`;
    setFeedback(msg);
  };

  const handleMicTap = () => {
    setOrbState('LISTENING_READY');
    setFeedback(
      settings.wakeWordEnabled
        ? language === 'bn'
          ? `"${settings.customWakeWord}" সক্রিয়। সেটিংস থেকে ভয়েস ও মাইক নিয়ন্ত্রণ করুন।`
          : `"${settings.customWakeWord}" active. Manage microphone & voice in Settings.`
        : language === 'bn'
          ? 'সেটিংস > ওয়েক ওয়ার্ড থেকে মাইক্রোফোন অনুমতি দিন।'
          : 'Grant Microphone access in Settings > Wake Word.'
    );
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = draft.trim();
    if (!trimmed) {
      setFeedback(language === 'bn' ? 'প্রথমে একটি বার্তা লিখুন।' : 'Type a message first.');
      return;
    }
    setDraft('');
    setOrbState('ACTIVE_GREETING');
    updateSettings((p) => ({ ...p, conversationCount: p.conversationCount + 1 }));
    setFeedback(
      language === 'bn'
        ? `"${trimmed}" হিস্টোরিতে সংরক্ষিত হয়েছে।`
        : `"${trimmed}" saved to conversation history.`
    );
  };

  const orbStateLabel =
    orbState === 'IDLE'
      ? language === 'bn'
        ? 'ট্যাপ করে পরীক্ষা করুন'
        : 'Tap orb to interact'
      : orbState === 'ACTIVE_GREETING'
        ? language === 'bn'
          ? 'অভ্যর্থনা মোড সক্রিয়'
          : 'Greeting state active'
        : language === 'bn'
          ? 'ভয়েস UI প্রস্তুত'
          : 'Voice UI ready';

  return (
    <div className="flex flex-col items-center">
      <div className="w-full max-w-[380px] rounded-[34px] border border-[#1E2B4D] bg-[#060B18] p-3 shadow-2xl shadow-cyan-950/20">
        <div
          className={`min-h-[670px] overflow-hidden rounded-[26px] border ${
            isDark ? 'border-[#141F3D] text-slate-100' : 'border-slate-200 text-slate-900'
          }`}
          style={{
            background: isDark
              ? 'linear-gradient(180deg, #060B18 0%, #091229 45%, #060B18 100%)'
              : 'linear-gradient(180deg, #F4F7FC 0%, #E4ECF8 45%, #F4F7FC 100%)',
          }}
        >
          {screen === 'settings' ? (
            <SettingsPhoneView
              settings={settings}
              onUpdate={updateSettings}
              onBack={() => setScreen('home')}
              isDark={isDark}
            />
          ) : (
            <div className="flex min-h-[670px] flex-col justify-between px-5 py-4">
              {/* Top Bar inside HomeScreen.kt */}
              <div>
                <div
                  className={`mb-4 flex items-center justify-between text-[11px] font-mono tabular-nums ${
                    isDark ? 'text-slate-400' : 'text-slate-500'
                  }`}
                >
                  <span>09:41</span>
                  <span>Android 15 · com.sara.assistant</span>
                </div>

                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-base font-bold tracking-tight">
                      {assistantName.toUpperCase()}
                    </div>
                    <div
                      className={`truncate text-xs ${isDark ? 'text-slate-400' : 'text-slate-600'}`}
                      style={{ fontFamily: 'var(--font-bengali)' }}
                    >
                      {language === 'bn'
                        ? `${settings.personalityMode === 'friendly' ? 'বন্ধুত্বপূর্ণ' : 'পেশাদার'} · ${settings.selectedModel}`
                        : `${settings.personalityMode === 'friendly' ? 'Friendly' : 'Professional'} · ${settings.selectedModel}`}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <div
                      className={`flex items-center rounded-[10px] border p-1 ${
                        isDark ? 'border-[#1E2B4D] bg-[#0D1529]' : 'border-slate-200 bg-white'
                      }`}
                      role="group"
                      aria-label="Language toggle"
                    >
                      <button
                        type="button"
                        onClick={() => setLanguage('bn')}
                        className="min-h-[32px] rounded-[7px] px-2.5 text-xs font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                        style={
                          language === 'bn'
                            ? { backgroundColor: `${accent.hex}2A`, color: accent.hex }
                            : undefined
                        }
                      >
                        বাংলা
                      </button>
                      <button
                        type="button"
                        onClick={() => setLanguage('en')}
                        className="min-h-[32px] rounded-[7px] px-2.5 text-xs font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                        style={
                          language === 'en'
                            ? { backgroundColor: `${accent.hex}2A`, color: accent.hex }
                            : undefined
                        }
                      >
                        EN
                      </button>
                    </div>

                    <button
                      type="button"
                      onClick={() => setScreen('settings')}
                      aria-label="Open SARA Settings"
                      className={`flex h-10 w-10 items-center justify-center rounded-xl border transition-colors cursor-pointer ${
                        isDark ? 'border-[#1E2B4D] bg-[#0D1529]' : 'border-slate-200 bg-white'
                      }`}
                      style={{ color: accent.hex }}
                    >
                      <Settings className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                {/* Bilingual Time-Aware Greeting */}
                <div className="mt-7 text-center">
                  <h2
                    className="text-[28px] font-semibold leading-tight"
                    style={{ fontFamily: 'var(--font-bengali)' }}
                  >
                    {greeting}
                  </h2>
                  <p
                    className={`mt-1.5 ${textScaleClass} ${isDark ? 'text-slate-400' : 'text-slate-600'}`}
                    style={{ fontFamily: 'var(--font-bengali)' }}
                  >
                    {language === 'bn'
                      ? `আমি ${assistantName}, আপনার ব্যক্তিগত এআই সহকারী`
                      : `I am ${assistantName}, your personal AI assistant`}
                  </p>
                </div>
              </div>

              {/* Center Focal Anchor: Animated SARA Orb */}
              <div className="my-4 flex flex-col items-center justify-center">
                <SaraOrbCanvas
                  state={orbState}
                  onTap={handleOrbTap}
                  size={200}
                  primaryRgb={accent.rgb}
                  secondaryRgb={accent.secondaryRgb}
                  animationsEnabled={settings.animationsEnabled}
                  canvasBg={isDark ? '#060B18' : '#F4F7FC'}
                />
                <div className="mt-2 text-xs font-medium" style={{ color: accent.hex }}>
                  {orbStateLabel}
                </div>
                <div
                  className={`mt-2 min-h-[36px] px-3 text-center text-xs leading-relaxed ${
                    isDark ? 'text-slate-400' : 'text-slate-600'
                  }`}
                >
                  {feedback ?? ''}
                </div>
              </div>

              {/* Bottom Status Card + Composer */}
              <div>
                <div
                  className={`mb-3 rounded-2xl border px-3.5 py-3 ${
                    isDark ? 'border-[#1E2B4D] bg-[#0D1529]' : 'border-slate-200 bg-white'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs">
                    <span>
                      {language === 'bn' ? 'ভয়েস ও সেটিংস সিস্টেম' : 'Voice & Settings System'}
                    </span>
                    <span
                      className={`font-medium ${
                        settings.voiceResponsesEnabled ? 'text-emerald-400' : 'text-slate-400'
                      }`}
                    >
                      {settings.voiceResponsesEnabled
                        ? language === 'bn'
                          ? 'সক্রিয়'
                          : 'Active'
                        : language === 'bn'
                          ? 'নীরব'
                          : 'Muted'}
                    </span>
                  </div>
                  <div className={`my-2.5 h-px ${isDark ? 'bg-[#1E2B4D]' : 'bg-slate-200'}`} />
                  <div className="flex items-center justify-between text-xs">
                    <span>
                      {language === 'bn'
                        ? `মেমোরি (${settings.memories.length}) · বার্তা (${settings.conversationCount})`
                        : `Memories (${settings.memories.length}) · History (${settings.conversationCount})`}
                    </span>
                    <span className="font-medium" style={{ color: accent.hex }}>
                      {settings.memoryEnabled
                        ? language === 'bn'
                          ? 'সংরক্ষণ চালু'
                          : 'Memory On'
                        : language === 'bn'
                          ? 'বন্ধ'
                          : 'Paused'}
                    </span>
                  </div>
                </div>

                <form
                  onSubmit={handleSend}
                  className={`flex items-center gap-1.5 rounded-[20px] border p-1.5 ${
                    isDark ? 'border-[#1E2B4D] bg-[#131D38]' : 'border-slate-200 bg-white'
                  }`}
                >
                  <input
                    type="text"
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    placeholder={
                      language === 'bn' ? `${assistantName}-কে কিছু লিখুন...` : `Message ${assistantName}...`
                    }
                    aria-label="Message SARA"
                    className="min-w-0 flex-1 bg-transparent px-3 py-2 text-sm placeholder:text-slate-500 focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={handleMicTap}
                    aria-label="Voice input preview"
                    className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full transition-colors cursor-pointer"
                    style={{ backgroundColor: `${accent.hex}24`, color: accent.hex }}
                  >
                    <Mic className="h-4 w-4" />
                  </button>
                  <button
                    type="submit"
                    aria-label="Send message preview"
                    className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-[#060B18] transition-colors cursor-pointer"
                    style={{ backgroundColor: accent.hex }}
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Quick Preview Controls below device frame */}
      <div className="mt-4 flex flex-wrap items-center justify-center gap-2 text-xs text-slate-400">
        <button
          type="button"
          onClick={() => setScreen(screen === 'home' ? 'settings' : 'home')}
          className="min-h-[36px] rounded-lg border border-[#1E2B4D] bg-[#0D1529] px-3 py-1.5 font-medium text-cyan-300 transition-colors hover:border-cyan-400/50 cursor-pointer"
        >
          {screen === 'home' ? 'Open Settings Screen' : 'Back to Home Screen'}
        </button>
        {screen === 'home' &&
          (['IDLE', 'ACTIVE_GREETING', 'LISTENING_READY'] as const).map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setOrbState(s)}
              className={`min-h-[36px] rounded-lg border px-3 py-1.5 font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                orbState === s
                  ? 'border-cyan-400/50 bg-cyan-400/15 text-cyan-300'
                  : 'border-[#1E2B4D] bg-[#0D1529] text-slate-400 hover:text-slate-200'
              }`}
            >
              {s === 'IDLE' ? 'Idle' : s === 'ACTIVE_GREETING' ? 'Greeting' : 'Voice UI'}
            </button>
          ))}
      </div>
    </div>
  );
}
