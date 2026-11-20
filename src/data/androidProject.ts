import settingsGradle from '../../android-sara/settings.gradle.kts?raw';
import rootBuildGradle from '../../android-sara/build.gradle.kts?raw';
import gradleProperties from '../../android-sara/gradle.properties?raw';
import libsVersionsToml from '../../android-sara/gradle/libs.versions.toml?raw';
import appBuildGradle from '../../android-sara/app/build.gradle.kts?raw';
import proguardRules from '../../android-sara/app/proguard-rules.pro?raw';
import androidManifest from '../../android-sara/app/src/main/AndroidManifest.xml?raw';
import stringsXml from '../../android-sara/app/src/main/res/values/strings.xml?raw';
import themesXml from '../../android-sara/app/src/main/res/values/themes.xml?raw';
import colorsXml from '../../android-sara/app/src/main/res/values/colors.xml?raw';
import saraApplicationKt from '../../android-sara/app/src/main/java/com/sara/assistant/SaraApplication.kt?raw';
import colorKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/theme/Color.kt?raw';
import typeKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/theme/Type.kt?raw';
import themeKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/theme/Theme.kt?raw';
import saraOrbKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/components/SaraOrb.kt?raw';
import settingsComponentsKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/components/SettingsComponents.kt?raw';
import homeScreenKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/screens/HomeScreen.kt?raw';
import settingsScreenKt from '../../android-sara/app/src/main/java/com/sara/assistant/ui/screens/SettingsScreen.kt?raw';
import mainActivityKt from '../../android-sara/app/src/main/java/com/sara/assistant/MainActivity.kt?raw';
import settingsModelsKt from '../../android-sara/app/src/main/java/com/sara/assistant/data/settings/SaraSettingsModels.kt?raw';
import secureKeyStorageKt from '../../android-sara/app/src/main/java/com/sara/assistant/data/settings/SecureKeyStorage.kt?raw';
import settingsRepoKt from '../../android-sara/app/src/main/java/com/sara/assistant/data/settings/SaraSettingsRepository.kt?raw';
import permissionHelperKt from '../../android-sara/app/src/main/java/com/sara/assistant/system/AndroidPermissionHelper.kt?raw';
import voiceEngineKt from '../../android-sara/app/src/main/java/com/sara/assistant/system/SaraVoiceEngine.kt?raw';
import wakeServiceKt from '../../android-sara/app/src/main/java/com/sara/assistant/system/SaraWakeService.kt?raw';
import geminiCheckerKt from '../../android-sara/app/src/main/java/com/sara/assistant/system/GeminiConnectionChecker.kt?raw';

export interface AndroidProjectFile {
  id: string;
  name: string;
  path: string;
  category: 'Gradle' | 'Manifest & Resources' | 'Kotlin Core' | 'Compose UI' | 'System & Data';
  language: string;
  roleEn: string;
  roleBn: string;
  content: string;
}

export const ANDROID_PROJECT_FILES: AndroidProjectFile[] = [
  {
    id: 'main-activity',
    name: 'MainActivity.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/MainActivity.kt',
    category: 'Kotlin Core',
    language: 'kotlin',
    roleEn: 'Wires SaraSettingsRepository, SaraVoiceEngine, device App Lock verification, and HomeScreen <-> SettingsScreen navigation.',
    roleBn: 'সেটিংস, ভয়েস ইঞ্জিন, অ্যাপ লক এবং হোম ও সেটিংস স্ক্রিনের নেভিগেশন পরিচালনা করে।',
    content: mainActivityKt,
  },
  {
    id: 'home-screen',
    name: 'HomeScreen.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/screens/HomeScreen.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Preserves original SARA Home Screen while adding Settings gear navigation and reactive personality, voice, theme, and memory updates.',
    roleBn: 'আগের মূল স্ক্রিনের ডিজাইন অক্ষুণ্ণ রেখে সেটিংস আইকন ও লাইভ সেটিংস যুক্ত করা হয়েছে।',
    content: homeScreenKt,
  },
  {
    id: 'settings-screen',
    name: 'SettingsScreen.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/screens/SettingsScreen.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Complete 10-category futuristic Settings screen with real Android permission launchers, Keystore API config, TTS test, memory manager, and live appearance controls.',
    roleBn: '১০টি সম্পূর্ণ ক্যাটাগরির সেটিংস স্ক্রিন—যেখানে প্রতিটি বাটন ও সুইচ বাস্তবে কাজ করে।',
    content: settingsScreenKt,
  },
  {
    id: 'settings-components',
    name: 'SettingsComponents.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/components/SettingsComponents.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Reusable Material 3 Settings cards, switches, sliders, dropdowns, segmented selectors, and live permission status rows.',
    roleBn: 'সেটিংস স্ক্রিনের কার্ড, সুইচ, স্লাইডার, ড্রপডাউন ও পারমিশন স্ট্যাটাস কম্পোনেন্ট।',
    content: settingsComponentsKt,
  },
  {
    id: 'sara-orb',
    name: 'SaraOrb.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/components/SaraOrb.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Custom Jetpack Compose Canvas animated AI Orb reacting to Accent Color and Animation Settings.',
    roleBn: 'অ্যাকসেন্ট রঙ ও অ্যানিমেশন সেটিংস অনুযায়ী চলমান এআই অর্ব।',
    content: saraOrbKt,
  },
  {
    id: 'settings-models',
    name: 'SaraSettingsModels.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/data/settings/SaraSettingsModels.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Immutable data models and enums covering AI Personality, Voice, Wake Word, AI Brain, Memory, Appearance, Notifications, and Security.',
    roleBn: 'সব ১০টি সেটিংস ক্যাটাগরির ডেটা মডেল ও অপশন।',
    content: settingsModelsKt,
  },
  {
    id: 'secure-key-storage',
    name: 'SecureKeyStorage.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/data/settings/SecureKeyStorage.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Encrypts API configuration using Android Keystore AES-256-GCM without ever exposing keys in UI or logs.',
    roleBn: 'Android Keystore দিয়ে নিরাপদভাবে API কী এনক্রিপ্ট করে রাখে।',
    content: secureKeyStorageKt,
  },
  {
    id: 'settings-repo',
    name: 'SaraSettingsRepository.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/data/settings/SaraSettingsRepository.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Persists all settings, saved memories, and conversation logs to private SharedPreferences with reactive StateFlows.',
    roleBn: 'সব সেটিংস, মেমোরি ও চ্যাট হিস্টোরি ফোনে সংরক্ষণ ও আপডেট করে।',
    content: settingsRepoKt,
  },
  {
    id: 'permission-helper',
    name: 'AndroidPermissionHelper.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/system/AndroidPermissionHelper.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Queries genuine Android runtime permissions, Accessibility Service, Notification Listener, Battery Optimization, and Device Lock status.',
    roleBn: 'অ্যান্ড্রয়েড সিস্টেম থেকে আসল পারমিশন ও সার্ভিসের অবস্থা যাচাই করে।',
    content: permissionHelperKt,
  },
  {
    id: 'voice-engine',
    name: 'SaraVoiceEngine.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/system/SaraVoiceEngine.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Real Android TextToSpeech integration supporting Bengali/English, speed, pitch, volume, and device voice availability.',
    roleBn: 'বাংলা ও ইংরেজি Text-to-Speech ভয়েস পরীক্ষা ও স্পিচ কন্ট্রোল।',
    content: voiceEngineKt,
  },
  {
    id: 'wake-service',
    name: 'SaraWakeService.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/system/SaraWakeService.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Android-compliant microphone foreground service gated by RECORD_AUDIO permission with transparent notification.',
    roleBn: 'মাইক্রোফোন অনুমতি সাপেক্ষে নিরাপদ ব্যাকগ্রাউন্ড ওয়েক ওয়ার্ড সার্ভিস।',
    content: wakeServiceKt,
  },
  {
    id: 'gemini-checker',
    name: 'GeminiConnectionChecker.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/system/GeminiConnectionChecker.kt',
    category: 'System & Data',
    language: 'kotlin',
    roleEn: 'Real background HTTP connection verifier for Gemini API or backend proxy without logging credentials.',
    roleBn: 'জেমিনি এপিআই বা ব্যাকএন্ড প্রক্সির সংযোগ পরীক্ষা করে।',
    content: geminiCheckerKt,
  },
  {
    id: 'sara-app',
    name: 'SaraApplication.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/SaraApplication.kt',
    category: 'Kotlin Core',
    language: 'kotlin',
    roleEn: 'App-wide Application root class.',
    roleBn: 'পুরো অ্যাপের মূল রুট ক্লাস।',
    content: saraApplicationKt,
  },
  {
    id: 'theme-color',
    name: 'Color.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/theme/Color.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Defines Dark Navy and Light Mode palettes plus Cyan, Violet, and semantic colors.',
    roleBn: 'ডার্ক নেভি ও লাইট মোড কালার প্যালেট।',
    content: colorKt,
  },
  {
    id: 'theme-type',
    name: 'Type.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/theme/Type.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Dynamic typography scale supporting Small, Medium, and Large text size preferences.',
    roleBn: 'লেখার আকার (ছোট/মাঝারি/বড়) অনুযায়ী ডাইনামিক টাইপোগ্রাফি।',
    content: typeKt,
  },
  {
    id: 'theme-kt',
    name: 'Theme.kt',
    path: 'SARA/app/src/main/java/com/sara/assistant/ui/theme/Theme.kt',
    category: 'Compose UI',
    language: 'kotlin',
    roleEn: 'Applies Dark/Light/System mode, Accent color, and Text scale reactively across all screens.',
    roleBn: 'থিম মোড, অ্যাকসেন্ট রঙ ও টেক্সট সাইজ পুরো অ্যাপে প্রয়োগ করে।',
    content: themeKt,
  },
  {
    id: 'manifest',
    name: 'AndroidManifest.xml',
    path: 'SARA/app/src/main/AndroidManifest.xml',
    category: 'Manifest & Resources',
    language: 'xml',
    roleEn: 'Declares runtime permissions (Microphone, Call, SMS, Notifications, Battery) and SaraWakeService.',
    roleBn: 'অ্যান্ড্রয়েডের প্রয়োজনীয় সব পারমিশন ও ব্যাকগ্রাউন্ড সার্ভিস ঘোষণা করে।',
    content: androidManifest,
  },
  {
    id: 'strings-xml',
    name: 'strings.xml',
    path: 'SARA/app/src/main/res/values/strings.xml',
    category: 'Manifest & Resources',
    language: 'xml',
    roleEn: 'Stores app name ("SARA") and core Bengali/English resource strings.',
    roleBn: 'অ্যাপের নাম ও মূল বাংলা/ইংরেজি টেক্সট রিসোর্স।',
    content: stringsXml,
  },
  {
    id: 'themes-xml',
    name: 'themes.xml',
    path: 'SARA/app/src/main/res/values/themes.xml',
    category: 'Manifest & Resources',
    language: 'xml',
    roleEn: 'Configures the native Android window background (#060B18) and transparent system bars.',
    roleBn: 'অ্যাপ চালুর সময় ডার্ক নেভি উইন্ডো ব্যাকগ্রাউন্ড সেট করে।',
    content: themesXml,
  },
  {
    id: 'colors-xml',
    name: 'colors.xml',
    path: 'SARA/app/src/main/res/values/colors.xml',
    category: 'Manifest & Resources',
    language: 'xml',
    roleEn: 'Provides XML color reference for themes.xml window background.',
    roleBn: 'XML থিমের জন্য নেভি ব্যাকগ্রাউন্ড রঙ।',
    content: colorsXml,
  },
  {
    id: 'app-gradle',
    name: 'app/build.gradle.kts',
    path: 'SARA/app/build.gradle.kts',
    category: 'Gradle',
    language: 'kotlin',
    roleEn: 'Configures applicationId (com.sara.assistant), SDK 26–35, version 0.2.0-settings, and Compose dependencies.',
    roleBn: 'অ্যাপ মডিউলের SDK ও Compose ডিপেন্ডেন্সি।',
    content: appBuildGradle,
  },
  {
    id: 'libs-toml',
    name: 'libs.versions.toml',
    path: 'SARA/gradle/libs.versions.toml',
    category: 'Gradle',
    language: 'toml',
    roleEn: 'Central Gradle version catalog keeping AGP, Kotlin 2.0, and Compose BOM versions aligned.',
    roleBn: 'সব লাইব্রেরি ও প্লাগইনের ভার্সন এক জায়গায় সাজিয়ে রাখে।',
    content: libsVersionsToml,
  },
  {
    id: 'root-gradle',
    name: 'build.gradle.kts',
    path: 'SARA/build.gradle.kts',
    category: 'Gradle',
    language: 'kotlin',
    roleEn: 'Top-level Gradle project file declaring Android and Kotlin Compose plugins.',
    roleBn: 'প্রজেক্টের মূল Gradle কনফিগারেশন ফাইল।',
    content: rootBuildGradle,
  },
  {
    id: 'settings-gradle',
    name: 'settings.gradle.kts',
    path: 'SARA/settings.gradle.kts',
    category: 'Gradle',
    language: 'kotlin',
    roleEn: 'Defines root project name ("SARA") and includes the :app module.',
    roleBn: 'প্রজেক্টের নাম ও :app মডিউল Gradle-কে চিনিয়ে দেয়।',
    content: settingsGradle,
  },
  {
    id: 'gradle-properties',
    name: 'gradle.properties',
    path: 'SARA/gradle.properties',
    category: 'Gradle',
    language: 'properties',
    roleEn: 'Enables AndroidX, UTF-8 encoding, and Gradle JVM memory settings.',
    roleBn: 'অ্যান্ড্রয়েডএক্স ও Gradle বিল্ডের মেমরি কনফিগারেশন সক্রিয় করে।',
    content: gradleProperties,
  },
  {
    id: 'proguard',
    name: 'proguard-rules.pro',
    path: 'SARA/app/proguard-rules.pro',
    category: 'Gradle',
    language: 'text',
    roleEn: 'Release build optimization rules for Kotlin and Jetpack Compose.',
    roleBn: 'রিলিজ APK তৈরির সময় কোড সেফটি রুল।',
    content: proguardRules,
  },
];

export interface RoadmapStep {
  step: number;
  title: string;
  titleBn: string;
  status: 'completed' | 'next' | 'planned';
  scope: string;
}

export const BUILD_ROADMAP: RoadmapStep[] = [
  {
    step: 1,
    title: 'Android Project Foundation & First Screen',
    titleBn: 'অ্যান্ড্রয়েড ফাউন্ডেশন ও প্রথম স্ক্রিন',
    status: 'completed',
    scope: 'Gradle setup, AndroidManifest, SaraTheme, animated SaraOrb, and bilingual HomeScreen.',
  },
  {
    step: 2,
    title: 'Complete 10-Module Settings System',
    titleBn: 'সম্পূর্ণ ১০-ক্যাটাগরি সেটিংস সিস্টেম',
    status: 'completed',
    scope: 'AI Personality, Voice/TTS, Wake Word Service, Real Android Permissions, Keystore API Config, Memory, Appearance, Notifications, Security, and About.',
  },
  {
    step: 3,
    title: 'Gemini AI Brain & Real Bengali Chat',
    titleBn: 'জেমিনি এআই ব্রেইন ও বাংলা চ্যাট',
    status: 'next',
    scope: 'Streaming Bengali/English conversation wired to selected AI Brain settings, search grounding, and context memory.',
  },
  {
    step: 4,
    title: 'Bengali Speech-to-Text & Phone Control',
    titleBn: 'বাংলা স্পিচ রিকগনিশন ও ফোন কন্ট্রোল',
    status: 'planned',
    scope: 'Live voice recognition, app launching, permission-gated calls/SMS confirmation, and Accessibility actions.',
  },
  {
    step: 5,
    title: 'Screen Vision (MediaProjection + Gemini)',
    titleBn: 'স্ক্রিন ভিশন ও স্ক্রিনশট বিশ্লেষণ',
    status: 'planned',
    scope: 'Explicit consent MediaProjection capture, error reading, and step-by-step Bengali visual guidance.',
  },
  {
    step: 6,
    title: 'Firebase Cloud Sync & Encrypted History',
    titleBn: 'ফায়ারবেস ক্লাউড মেমোরি ও ব্যাকআপ',
    status: 'planned',
    scope: 'Cloud sync for user-approved memories and conversation history.',
  },
];

export function buildTermuxBootstrapScript(): string {
  const lines: string[] = [
    '#!/usr/bin/env bash',
    'set -euo pipefail',
    '',
    'echo "Creating SARA Android Project (Foundation + Settings System)..."',
    '',
  ];

  for (const file of ANDROID_PROJECT_FILES) {
    const dir = file.path.substring(0, file.path.lastIndexOf('/'));
    lines.push(`mkdir -p "${dir}"`);
    lines.push(`cat <<'__SARA_EOF__' > "${file.path}"`);
    lines.push(file.content.replace(/\r\n/g, '\n').trimEnd());
    lines.push('__SARA_EOF__');
    lines.push('');
  }

  lines.push('echo "Done! Open the SARA folder in AndroidIDE or build with Gradle."');
  return lines.join('\n');
}
