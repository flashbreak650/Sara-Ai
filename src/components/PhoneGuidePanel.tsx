import { Download, Smartphone, Terminal } from 'lucide-react';
import { BUILD_ROADMAP } from '../data/androidProject';

interface PhoneGuidePanelProps {
  onDownloadScript: () => void;
}

export function PhoneGuidePanel({ onDownloadScript }: PhoneGuidePanelProps) {
  return (
    <div className="space-y-6">
      {/* Android-Only Phone Development Guide */}
      <div className="rounded-2xl border border-[#1E2B4D] bg-[#0D1529] p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold text-slate-100">
              How to Build This APK Using Only Your Android Phone
            </h3>
            <p
              className="mt-1 text-sm text-slate-400"
              style={{ fontFamily: 'var(--font-bengali)' }}
            >
              কম্পিউটার ছাড়াই শুধু আপনার অ্যান্ড্রয়েড ফোন ব্যবহার করে ধাপ ১-এর প্রজেক্ট তৈরি ও বিল্ড করার সহজ নিয়ম।
            </p>
          </div>
          <button
            type="button"
            onClick={onDownloadScript}
            className="flex min-h-[42px] items-center gap-2 rounded-xl bg-cyan-400 px-4 py-2 text-xs font-semibold text-[#060B18] transition-colors hover:bg-cyan-300 whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Download className="h-4 w-4" />
            <span>Download Termux Script</span>
          </button>
        </div>

        <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="rounded-xl border border-[#1E2B4D] bg-[#060B18] p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-cyan-300">
              <Smartphone className="h-4 w-4" />
              <span>Option A: AndroidIDE (Direct APK on Phone)</span>
            </div>
            <ol className="mt-3 space-y-2 text-xs leading-relaxed text-slate-300 list-decimal list-inside">
              <li>Install AndroidIDE (from GitHub Releases / F-Droid) on your Android phone.</li>
              <li>Open Termux (or AndroidIDE terminal) and run the downloaded <code className="font-mono text-cyan-300">create_sara_step1.sh</code> script to create the <code className="font-mono text-cyan-300">SARA/</code> folder with all 16 files.</li>
              <li>In AndroidIDE, tap <strong>Open Project</strong> and select the <code className="font-mono text-cyan-300">SARA</code> directory.</li>
              <li>Wait for Gradle sync, then press the Run button to compile and install the real <code className="font-mono text-cyan-300">app-debug.apk</code>.</li>
            </ol>
          </div>

          <div className="rounded-xl border border-[#1E2B4D] bg-[#060B18] p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-violet-300">
              <Terminal className="h-4 w-4" />
              <span>Option B: Termux + GitHub Actions (No Heavy RAM)</span>
            </div>
            <ol className="mt-3 space-y-2 text-xs leading-relaxed text-slate-300 list-decimal list-inside">
              <li>If your phone has lower RAM (4 GB or less), create the <code className="font-mono text-violet-300">SARA/</code> folder in Termux using <code className="font-mono text-violet-300">bash create_sara_step1.sh</code>.</li>
              <li>Push the <code className="font-mono text-violet-300">SARA/</code> project to a private GitHub repository from your phone.</li>
              <li>Use a free GitHub Actions Android Gradle workflow to build the <code className="font-mono text-violet-300">.apk</code> in the cloud and download it directly to your phone.</li>
              <li>Verify Screen 1 installs and opens smoothly before starting Step 2.</li>
            </ol>
          </div>
        </div>
      </div>

      {/* Step-by-Step Build Roadmap */}
      <div className="rounded-2xl border border-[#1E2B4D] bg-[#0D1529] p-5">
        <h3 className="text-lg font-semibold text-slate-100">
          Step-by-Step SARA Development Roadmap
        </h3>
        <p className="mt-1 text-xs text-slate-400">
          Each step is verified and installable before moving to the next capability.
        </p>

        <div className="mt-4 divide-y divide-[#1E2B4D]">
          {BUILD_ROADMAP.map((item) => (
            <div
              key={item.step}
              className="flex flex-col gap-1.5 py-3.5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-slate-100">
                  <span className="font-mono tabular-nums text-cyan-300">Step {item.step}</span>
                  <span aria-hidden="true" className="text-slate-600">·</span>
                  <span>{item.title}</span>
                </div>
                <div
                  className="mt-0.5 text-xs text-slate-400"
                  style={{ fontFamily: 'var(--font-bengali)' }}
                >
                  {item.titleBn} — {item.scope}
                </div>
              </div>

              <div className="shrink-0 text-xs font-medium">
                {item.status === 'completed' ? (
                  <span className="text-emerald-400">Built in Step 1</span>
                ) : item.status === 'next' ? (
                  <span className="text-cyan-300">Up Next</span>
                ) : (
                  <span className="text-slate-500">Planned</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
