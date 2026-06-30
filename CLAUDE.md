# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is a React Native Turbo Module (New Architecture) library that provides GPU-accelerated progressive blur effects on Android and iOS. It is a monorepo managed with Yarn workspaces — the library lives in the root, and a demo app lives in `example/`.

The library is scaffolded with `create-react-native-library` and built with `react-native-builder-bob`. It targets the New Architecture only (Fabric + JSI via Turbo Modules).

## Commands

Run from the root unless noted.

```sh
yarn               # install all dependencies (root + example workspace)
yarn typecheck     # TypeScript check
yarn lint          # ESLint
yarn lint --fix    # auto-fix formatting
yarn test          # Jest
yarn prepare       # build lib/ from src/ (runs react-native-builder-bob)

# Example app
yarn example start            # Metro bundler
yarn example ios              # run on iOS simulator
yarn example android          # run on Android emulator/device
yarn example web              # run on web (Vite)
yarn example build:web        # production web build

yarn release       # bump version, tag, publish to npm (uses release-it)
```

To run a single Jest test file:
```sh
yarn test src/__tests__/index.test.tsx
```

## Architecture

### Turbo Module pattern
- `src/NativeProgressiveBlur.ts` — the JS-side Turbo Module spec (`TurboModule` interface + `TurboModuleRegistry.getEnforcing`). This file drives codegen via the `codegenConfig` in `package.json` (spec name: `ProgressiveBlurSpec`).
- `src/multiply.native.tsx` — native-backed implementation: imports the Turbo Module and delegates to it.
- `src/multiply.tsx` — web/non-native fallback (plain JS, no native dependency).
- `src/index.tsx` — public API; re-exports from `./multiply` (platform resolution picks `.native.tsx` on device).

### Native layer
- **iOS**: `ios/ProgressiveBlur.h` + `ios/ProgressiveBlur.mm` — ObjC++ implementation conforming to the generated spec (`NativeProgressiveBlurSpecJSI`). Registered via `install_modules_dependencies` in `ProgressiveBlur.podspec`.
- **Android**: `android/build.gradle` — Kotlin/Android library using `com.facebook.react` plugin; min SDK 24, compile SDK 36, Kotlin 2.0.21.

### Build output
`react-native-builder-bob` compiles `src/` into `lib/` with two targets:
- `lib/module/` — ESM modules (used at runtime via `"main"` and `"exports"` in `package.json`)
- `lib/typescript/` — type declarations (used via `"types"`)

The `lib/` directory is gitignored; run `yarn prepare` to generate it locally.

### Pre-commit hooks (lefthook)
- ESLint runs on staged `*.{js,ts,jsx,tsx}` files.
- TypeScript (`tsc`) runs on staged files.
- `commitlint` enforces conventional commits on the commit message.

Commits must follow the [Conventional Commits](https://www.conventionalcommits.org) spec (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).

## Key constraints

- **Yarn only** — Yarn workspaces are used; `npm` will not work for development.
- **New Architecture only** — this module uses JSI/Turbo Modules and is not compatible with the legacy bridge.
- Native code changes require rebuilding the example app (`yarn example ios` / `yarn example android`); JS changes hot-reload via Metro.
- To edit native iOS source in Xcode: open `example/ios/ProgressiveBlurExample.xcworkspace` and navigate to `Pods > Development Pods > react-native-progressive-blur`.
- To edit native Android source in Android Studio: open `example/android` and find sources under `react-native-progressive-blur`.

# agent-device

Use agent-device only for app/device automation tasks.
Before planning device work, run `agent-device --version` and read `agent-device help workflow`.
For exploratory QA, read `agent-device help dogfood`.
For logs, network, traces, or runtime failures, read `agent-device help debugging`.
For React Native component trees, props/state/hooks, slow renders, or rerenders, read `agent-device help react-devtools`.
For React Native JavaScript heap growth, heap snapshots, or retained-object leaks, read `agent-device help cdp`.
For React Native apps, overlays, Metro/Fast Refresh blockers, and routing to React DevTools or debugging evidence, read `agent-device help react-native`.

Use the CLI in the integrated terminal.
If `agent-device` is not on PATH but the user installed it globally in another shell, resolve the absolute binary path instead of using `npx -y agent-device@latest`.
Prefer `open -> snapshot -i -> act -> re-snapshot -> verify -> close`.
Keep mutating commands against one session serial.
