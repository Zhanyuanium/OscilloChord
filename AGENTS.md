# AGENTS.md — OscilloChord

## Rules

1. **Build after every change.** Run `./gradlew :app:assembleDebug` before considering a task complete. Never commit code that doesn't compile.

2. **Run tests.** Run `./gradlew :app:testDebugUnitTest` before committing. All tests must pass.

3. **Do not mix formatting and logic changes.** Use ktlint to format code in a separate commit. Logic commits should only contain the minimal lines changed.

4. **Do not reformat code you are not changing.** Keep diffs small. The user reviews every change.

5. **UI changes require human confirmation.** The user must visually verify UI behavior on a real device before a UI change is considered done. Note this in commit messages for UI-related commits.

6. **Package name is always `me.doubao.oscillochord`.** Never use `com.example`, `io.oscillochord`, or any other package.

7. **Use `JAVA_HOME` when running Gradle.** Set `export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` before running `./gradlew`.

8. **Commits should be atomic and well-described.** Each commit should be a single logical change. Commit messages must be in English, use imperative mood, and include `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

9. **When debugging, add targeted logging.** Use `android.util.Log` with a consistent TAG. Remove debug logging before committing the fix.

10. **Read project memory before starting.** Check `CLAUDE.md` for architecture patterns and known pitfalls. Check the project memory directory for accumulated knowledge.
