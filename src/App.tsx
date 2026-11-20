import { useState } from 'react';
import { Download } from 'lucide-react';
import { AndroidPhonePreview } from './components/AndroidPhonePreview';
import { CodeExplorer } from './components/CodeExplorer';
import { PhoneGuidePanel } from './components/PhoneGuidePanel';
import { ANDROID_PROJECT_FILES, buildTermuxBootstrapScript } from './data/androidProject';

type WorkspaceTab = 'files' | 'guide';

export default function App() {
  const [activeTab, setActiveTab] = useState<WorkspaceTab>('files');

  const handleDownloadScript = () => {
    const script = buildTermuxBootstrapScript();
    const blob = new Blob([script], { type: 'text/x-shellscript;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'create_sara_step1.sh';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="min-h-screen bg-[#060B18] text-slate-100">
      {/* Strict 3-Zone Top Bar */}
      <header className="border-b border-[#1E2B4D] bg-[#060B18]/90 backdrop-blur-md sticky top-0 z-30">
        <div className="mx-auto flex max-w-[1380px] items-center justify-between gap-8 px-4 py-3.5 sm:px-6">
          <a
            href="#top"
            className="text-lg font-bold tracking-tight text-slate-50 whitespace-nowrap shrink-0"
          >
            SARA Studio
          </a>

          <nav className="flex items-center gap-5 text-sm font-medium">
            <button
              type="button"
              onClick={() => setActiveTab('files')}
              className={`min-h-[36px] border-b-2 px-1 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                activeTab === 'files'
                  ? 'border-cyan-400 text-cyan-300'
                  : 'border-transparent text-slate-400 hover:text-slate-100'
              }`}
            >
              Project Files
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('guide')}
              className={`min-h-[36px] border-b-2 px-1 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                activeTab === 'guide'
                  ? 'border-cyan-400 text-cyan-300'
                  : 'border-transparent text-slate-400 hover:text-slate-100'
              }`}
            >
              Phone Build Guide
            </button>
          </nav>

          <button
            type="button"
            onClick={handleDownloadScript}
            className="flex min-h-[40px] items-center gap-2 rounded-lg bg-cyan-400 px-4 py-2 text-xs font-semibold text-[#060B18] transition-colors hover:bg-cyan-300 whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Download className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Export Termux Script</span>
            <span className="sm:hidden">Export</span>
          </button>
        </div>
      </header>

      {/* Main Responsive Workspace */}
      <main id="top" className="mx-auto max-w-[1380px] px-4 py-6 sm:px-6 lg:py-8">
        {/* Intro Strip */}
        <div className="mb-8 flex flex-col gap-2 border-b border-[#1E2B4D] pb-6">
          <div className="flex flex-wrap items-center gap-2 text-xs text-slate-400">
            <span>Step 2 · Settings System</span>
            <span aria-hidden="true">·</span>
            <span>Native Kotlin & Jetpack Compose</span>
            <span aria-hidden="true">·</span>
            <span className="font-mono tabular-nums">{ANDROID_PROJECT_FILES.length} Project Files</span>
            <span aria-hidden="true">·</span>
            <span>Package: com.sara.assistant</span>
          </div>
          <h1 className="max-w-3xl text-2xl font-semibold tracking-tight text-slate-50 sm:text-3xl">
            SARA Personal AI Assistant — Home & 10-Module Settings System
          </h1>
          <p className="max-w-3xl text-sm leading-relaxed text-slate-400">
            Tap the Settings icon inside the live phone preview to test all 10 interactive Settings modules alongside the updated native Kotlin, Keystore security, TextToSpeech, and Android permission files.
          </p>
        </div>

        {/* 2-Column Split: Live Android Screen Preview + Complete Project Files / Guide */}
        <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-12">
          <section className="lg:col-span-5" aria-label="Interactive Android Screen 1 Preview">
            <AndroidPhonePreview />
          </section>

          <section className="lg:col-span-7" aria-label="Android Project Files and Guide">
            {activeTab === 'files' ? (
              <CodeExplorer />
            ) : (
              <PhoneGuidePanel onDownloadScript={handleDownloadScript} />
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
