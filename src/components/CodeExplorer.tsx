import { useMemo, useState } from 'react';
import { Check, Copy, Download, FileCode2, FolderTree } from 'lucide-react';
import { ANDROID_PROJECT_FILES, type AndroidProjectFile } from '../data/androidProject';

const CATEGORIES: Array<'All' | AndroidProjectFile['category']> = [
  'All',
  'Compose UI',
  'System & Data',
  'Kotlin Core',
  'Manifest & Resources',
  'Gradle',
];

export function CodeExplorer() {
  const [selectedId, setSelectedId] = useState<string>(ANDROID_PROJECT_FILES[0].id);
  const [category, setCategory] = useState<'All' | AndroidProjectFile['category']>('All');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const filteredFiles = useMemo(() => {
    if (category === 'All') return ANDROID_PROJECT_FILES;
    return ANDROID_PROJECT_FILES.filter((f) => f.category === category);
  }, [category]);

  const activeFile = useMemo(
    () => ANDROID_PROJECT_FILES.find((f) => f.id === selectedId) ?? ANDROID_PROJECT_FILES[0],
    [selectedId]
  );

  const handleCopy = async (file: AndroidProjectFile) => {
    try {
      await navigator.clipboard.writeText(file.content);
      setCopiedId(file.id);
      window.setTimeout(() => setCopiedId((curr) => (curr === file.id ? null : curr)), 1800);
    } catch {
      // Fallback textarea copy if clipboard API is restricted
      const el = document.createElement('textarea');
      el.value = file.content;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
      setCopiedId(file.id);
      window.setTimeout(() => setCopiedId((curr) => (curr === file.id ? null : curr)), 1800);
    }
  };

  const handleDownloadFile = (file: AndroidProjectFile) => {
    const blob = new Blob([file.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.name.includes('/') ? file.name.split('/').pop() ?? 'file.txt' : file.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const lines = useMemo(() => activeFile.content.replace(/\r\n/g, '\n').split('\n'), [activeFile]);

  return (
    <div className="rounded-2xl border border-[#1E2B4D] bg-[#0D1529] overflow-hidden">
      {/* Category Filter Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#1E2B4D] px-4 py-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-100">
          <FolderTree className="h-4 w-4 text-cyan-400" />
          <span>Step 1 Native Android Files ({ANDROID_PROJECT_FILES.length})</span>
        </div>

        <div className="flex flex-wrap items-center gap-1 rounded-lg bg-[#060B18] p-1 border border-[#1E2B4D]">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setCategory(cat)}
              className={`min-h-[32px] rounded-md px-2.5 py-1 text-xs font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                category === cat
                  ? 'bg-[#131D38] text-cyan-300'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Two-pane File Tree + Code Viewer */}
      <div className="grid grid-cols-1 md:grid-cols-12">
        {/* File List */}
        <div className="border-b border-[#1E2B4D] md:col-span-4 md:border-b-0 md:border-r max-h-[540px] overflow-y-auto">
          {filteredFiles.map((file) => {
            const isSelected = file.id === activeFile.id;
            return (
              <button
                key={file.id}
                type="button"
                onClick={() => setSelectedId(file.id)}
                className={`flex w-full items-start gap-2.5 border-b border-[#1E2B4D]/60 px-3.5 py-3 text-left transition-colors cursor-pointer ${
                  isSelected ? 'bg-[#131D38]' : 'hover:bg-[#131D38]/50'
                }`}
              >
                <FileCode2
                  className={`mt-0.5 h-4 w-4 shrink-0 ${
                    isSelected ? 'text-cyan-400' : 'text-slate-500'
                  }`}
                />
                <div className="min-w-0 flex-1">
                  <div
                    className={`truncate text-xs font-semibold ${
                      isSelected ? 'text-cyan-300' : 'text-slate-200'
                    }`}
                  >
                    {file.name}
                  </div>
                  <div className="mt-0.5 truncate font-mono text-[11px] text-slate-500">
                    {file.path}
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* File Details & Code View */}
        <div className="md:col-span-8 flex flex-col">
          <div className="border-b border-[#1E2B4D] bg-[#091022] p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <div className="font-mono text-xs text-cyan-300 break-all">{activeFile.path}</div>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-300">{activeFile.roleEn}</p>
                <p
                  className="mt-1 text-xs leading-relaxed text-slate-400"
                  style={{ fontFamily: 'var(--font-bengali)' }}
                >
                  {activeFile.roleBn}
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  type="button"
                  onClick={() => handleCopy(activeFile)}
                  className="flex min-h-[38px] items-center gap-1.5 rounded-lg border border-[#1E2B4D] bg-[#131D38] px-3 py-1.5 text-xs font-medium text-slate-200 transition-colors hover:border-cyan-400/50 hover:text-cyan-300 whitespace-nowrap shrink-0 cursor-pointer"
                >
                  {copiedId === activeFile.id ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-emerald-400" />
                      <span>Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5" />
                      <span>Copy</span>
                    </>
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => handleDownloadFile(activeFile)}
                  className="flex min-h-[38px] items-center gap-1.5 rounded-lg border border-[#1E2B4D] bg-[#131D38] px-3 py-1.5 text-xs font-medium text-slate-200 transition-colors hover:border-cyan-400/50 hover:text-cyan-300 whitespace-nowrap shrink-0 cursor-pointer"
                >
                  <Download className="h-3.5 w-3.5" />
                  <span>Save</span>
                </button>
              </div>
            </div>
          </div>

          {/* Code block with tabular line numbers */}
          <div className="max-h-[430px] overflow-auto bg-[#060B18] p-4 font-mono text-xs leading-6">
            <table className="w-full border-collapse">
              <tbody>
                {lines.map((line, index) => (
                  <tr key={index} className="hover:bg-[#0D1529]/70">
                    <td className="w-10 select-none pr-4 text-right align-top tabular-nums text-slate-600">
                      {index + 1}
                    </td>
                    <td className="whitespace-pre text-slate-200">{line || ' '}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
