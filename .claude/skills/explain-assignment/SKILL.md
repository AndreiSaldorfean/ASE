---
name: explain-assignment
description: After an assignment has been implemented, verify the implementation (tests + live checks), fix any bugs found, then write a deep, beginner-friendly solution.md for it in assignments/<N>_<name>/, explaining both the assignment and how it was implemented in the instakill codebase, for a reader who is not a web developer. Requires a valid description.md and existing implementation code; refuses otherwise. Use when the user asks to explain, document or write up a finished assignment's solution, or invokes /explain-assignment.
argument-hint: "[assignment folder, e.g. 3_upload_feature]"
---

# Explain an assignment in solution.md

You write `assignments/<N>_<name>/solution.md`. It explains **what the assignment asked** and **how the code in this repo solves it**. Before writing, you **verify** the implementation and **fix any bugs you find**, so the document explains code that actually works. Assume the reader:

- is new to the topic and **is not a web developer**,
- can read a little code but doesn't know Spring, Vue, HTTP, JSON or browsers' inner workings,
- wants to understand *why* things are done, not only *what* was done.

The level of detail is deep. When in doubt, explain more, but never pad: every paragraph must teach something.

## 0. Preconditions: stop if they aren't met

This skill works on **finished work**. It never implements the assignment itself and never writes a solution in advance. The only code it changes is **bug fixes** found during verification (step 3). Before doing anything, check both conditions. If either fails, **do not create or modify solution.md and do not change any code**. Tell the user exactly what's missing and stop.

1. **Valid description.** `assignments/<N>_<name>/description.md` exists, is not empty, and actually describes an assignment: it has a goal and at least one concrete requirement, task or acceptance criterion. A placeholder, a single vague line or unrelated text doesn't count.
2. **Code has been written.** The implementation for this assignment exists in the repo (committed on the branch or in the working tree, found as described in step 2). There must be real code changes that address the description's requirements. A branch with no relevant changes, or only the assignment files themselves, doesn't count.
   - If the implementation looks clearly **incomplete** (e.g. backend done but the required frontend part missing), stop and ask the user whether to wait or write the explanation anyway, flagging the missing parts.
   - Small gaps in *optional* extras are fine. Document them honestly (see step 2.6).

## 1. Find the assignment

Assignments are organized like this:

```
assignments/
  <N>_<short_name>/        e.g. 3_upload_feature/
    description.md         the assignment text (source of truth for requirements)
    solution.md            the file you write
```

- If the user named an assignment (argument or message), use that folder.
- Otherwise list `assignments/` and pick the one whose `solution.md` is empty or missing. If several fit, ask which one.
- Read `description.md` fully. Every goal, acceptance criterion, task and optional extra in it must be addressed in the solution.
- If `solution.md` already has content, ask before overwriting it.

## 2. Find the implementation

Never describe code from memory or from what the assignment *suggests*. Describe the code that actually exists.

1. Find the changes for this assignment:
   - `git log --oneline main..HEAD` and `git diff main...HEAD --stat` for committed work on the branch (branches are named like `assignment-<N>`),
   - `git status` and `git diff` for uncommitted work,
   - if that's unclear, search the code for the names used in `description.md` (endpoints, classes, property keys).
2. Write down the **list of changes** for this assignment: every new file, and for each modified file the specific hunks (`git diff`) that changed. Exclude anything not related to the assignment, such as earlier assignments' commits, the `assignments/` folder itself and unrelated refactors. **This list is the backbone of the explanation.**
3. Read every changed or new file in full, so you understand each change in context. Also read the existing code it plugs into (e.g. security config, exception handler, the API client, the component that uses it). Use that existing code only as background to explain how a change fits in; never present it as part of the solution.
4. Note the project's layers so you can explain where each piece sits. The backend (`backend/`, Spring Boot, Java) uses packages per feature, such as `post/`, `user/`, `media/`, each with `api/rest` (HTTP layer), `application` (logic) and `domain`, plus `infrastructure/config` for wiring. The frontend (`frontend/`, Vue 3 + Vite) has `src/api.js` for HTTP calls and `src/App.vue` for the UI.
5. Look for tests covering the feature (`backend/src/test/...`) and read them; they are good evidence of behavior.
6. Re-check precondition 2 now that you've read the code. If required parts are missing, stop as described in step 0. A missing required part is unfinished work, not a bug: don't implement it. For minor differences, such as a skipped optional extra or a requirement met in a different way than the assignment suggested, say so honestly in the document. Don't paper over gaps.

## 3. Verify the implementation and fix bugs

Don't trust that the code works because it compiles or because tests exist. Check it yourself, then fix what's broken.

### 3.1 Verify

1. **Run the full automated test suite** (`cd backend && ./gradlew test`; read the results in `backend/build/test-results/test/*.xml`). Build the frontend (`cd frontend && npx vite build`; run `npm ci` first if `node_modules` is missing).
2. **Exercise the feature against a real running server** (`./gradlew bootRun`, or a `@SpringBootTest(webEnvironment = RANDOM_PORT)` test). Use curl for the happy path **and every error path the assignment or code implies**, for example: each validation rule, not logged in, wrong or missing field, wrong body type, limits exceeded (lower limits temporarily via environment variables such as `SPRING_SERVLET_MULTIPART_MAXFILESIZE=1KB`; `--args` with `bootRun` isn't reliable). Check the **status code and the body** of each response against what the code intends.
   - MockMvc tests **do not** simulate the servlet container: errors raised before a controller is chosen (multipart limits, unsupported content type, unknown path) are forwarded to `/error` only in a real server, and go through Spring Security again there. Always check those cases on a real server.
3. **Review the changed code for bugs**: error handling that never triggers, security rules that block or expose too much, UI dead ends (states the user can't get out of), browser differences, unhandled failures, and mismatches between frontend and backend (field names, URLs, proxy rules).
4. When starting servers yourself: use the scratchpad for test files, stop every server you started afterwards (kill by PID, not with a `pkill -f` pattern that can match your own shell), and delete only files you created. Never delete the user's own data (e.g. files they uploaded).

### 3.2 Bug or limitation?

- A **bug** is code that doesn't do what it was clearly meant to do, or that breaks a requirement, or that leaves the user stuck. Examples: a handler that never runs, a wrong status code, a button that can't be undone, a crash. **Fix bugs** (3.3).
- A **design limitation** is a reasonable choice with a trade-off, such as uploading on submit instead of on file selection, or not cleaning up orphaned files. **Don't change it.** Document it in the "Known limitations" part of the document.
- A **missing required part** is not a bug (see step 2.6).

### 3.3 Fix bugs

For each bug:

1. Find the **root cause**, not just the symptom. Confirm it (logs, a minimal reproduction).
2. Where feasible, **write a failing regression test first** that reproduces the bug, run it, and confirm it fails for the right reason.
3. **Fix it** in the style of the surrounding code (naming, comment density, package layout). Keep the fix minimal and inside the scope of the assignment's feature. Don't refactor unrelated code.
4. Run the **whole** test suite again and rebuild the frontend. Repeat the live check that exposed the bug.
5. Record the bug for the document: symptom, root cause, fix, and how it's now verified.

After fixing, **go back to step 2** and rebuild the list of changes from git: the fixes are now part of the assignment's changes. Then verify again (3.1). Repeat until no bugs remain. Only then write the document.

If a fix would be large, risky, or change the assignment's intended behavior, don't make it. Ask the user instead.

## 4. Structure of solution.md

Use this outline. Adapt headings to the assignment, but keep every section.

```markdown
# <Assignment title> – Solution explained

> One-paragraph summary: what the feature does, in everyday words.

## Table of contents

## 1. The big picture
   - What problem the assignment solves, with a real-life analogy.
   - The pieces involved (browser, frontend, backend, disk/database) and how they talk.
   - A Mermaid diagram of the overall architecture.

## 2. Background concepts you need
   - Explain each concept the solution relies on, from zero, BEFORE it's used.
     (e.g. client/server, HTTP request/response, methods like GET/POST, status codes,
      URLs and paths, JSON, multipart/form-data, configuration files, authentication/cookies,
      static file serving, dev-server proxy...). Only those relevant to this assignment.
   - One short subsection per concept: what it is, an analogy, a tiny concrete example.

## 3. What the assignment asks for
   - Restate the goal and each requirement in plain language.
   - Explain *why* each requirement exists (what would break or be missing without it).

## 4. The solution, step by step
   - Follow the flow of data (e.g. user clicks → frontend → backend → disk → back to browser),
     not the order of files.
   - A Mermaid sequence diagram of the main flow.
   - One subsection per entry in the list of changes from step 2. Every changed file must be
     covered, and nothing that wasn't changed is presented as part of the solution.
     For a modified file, show and explain only the changed part (with a line or two of
     surrounding code for orientation), and say what it looked like before if that helps.
     Each subsection has:
       - **File:** `path/to/File.java` (clickable relative link), marked *new* or *modified*
       - **What it does**, in one or two sentences.
       - **Why it's needed**, i.e. which requirement it satisfies.
       - **The code**: a real snippet copied verbatim from the file.
       - **Line-by-line walkthrough**: a numbered or bulleted explanation of the snippet in
         everyday language. Explain annotations (`@RestController`, `@PostMapping`...),
         keywords and library calls the first time they appear.
       - **What happens if something goes wrong** (errors, edge cases), if relevant.

## 5. Optional extras
   - What was done for the optional parts (or that they weren't done), explained the same way.

## 6. Verification: bugs found and fixed, known limitations
   - How the implementation was verified (tests, live checks, what was checked).
   - For each bug fixed in step 3: the symptom (what the user would have seen), the root cause
     explained in plain words, the fix, and the test that now guards it. Link to the section
     of chapter 4 that explains the fixed code. Write "No bugs found" if there were none.
   - Known limitations (design trade-offs deliberately left as they are), and anything not verified.

## 7. Requirement → code checklist
   | Requirement (from description.md) | Where it's implemented | How it's verified |
   One row per acceptance criterion / task. Mark anything missing or partial.

## 8. How to run and test it yourself
   - Exact commands to start backend and frontend, with what you should see.
   - Click-by-click steps to try the feature in the browser.
   - A command-line alternative (curl) with expected output.
   - How to run the automated tests and what each relevant test checks.
   - Common problems and fixes (port in use, not logged in, file too large...).

## 9. Glossary
   - Alphabetical list of every technical term used in the document, one or two plain sentences each.
```

## 5. Writing rules

- **Define before use.** The first time a technical term appears, explain it in plain words or link to its section/glossary entry. Never assume the reader knows Spring, REST, Vue, HTTP or JSON.
- **Use analogies.** For example: the server is a restaurant kitchen, an endpoint is a counter window, multipart upload is a parcel with a label, a cookie is a wristband. Keep each analogy accurate and drop it when it stops helping.
- **Concrete over abstract.** Show real example requests and responses (with actual URLs, JSON, status codes) next to the explanations.
- **Snippets are verbatim** from the current files, trimmed with `// ...` if long. Put the file path above each snippet. Use the right fence language (`java`, `yaml`, `js`, `vue`, `bash`, `http`).
- **Explain the "why".** For each design choice, say why it was made and what the alternative would be (e.g. why files get a random UUID name instead of the original name).
- **Short paragraphs**, headings every few paragraphs, bullet lists for steps. Bold the key idea of a paragraph when it helps skimming.
- **Mermaid diagrams**: at least one architecture/flowchart and one `sequenceDiagram` of the main request flow. Keep labels short and use plain words, and make sure the syntax is valid (quote labels containing special characters like `/`, `:`, `{`, `(`).
- **Links**: refer to code with relative links from the solution file, e.g. `[MediaController.java](../../backend/src/main/java/com/instakill/media/api/rest/MediaController.java)`.
- **Honesty**: if something wasn't verified (e.g. not tested in a real browser), say so.
- **Describe the code as it is after the fixes.** Chapter 4 explains the final, fixed code. The bug history goes in chapter 6, not scattered through chapter 4 as warnings.
- Write in English unless the user asks otherwise.

## 6. Check the document before finishing

- The full test suite passes and the frontend builds, after the last fix.
- Every bug fixed in step 3 is described in chapter 6, and its regression test is in the list of changes.
- Every requirement in `description.md` appears in the checklist table.
- Every entry in the list of changes (step 2, rebuilt after fixes) is explained in chapter 4, and every file cited in chapters 4–7 as part of the solution is actually in that list.
- Every file path and link points to a file that exists (check with `ls`).
- Every snippet matches the current file content.
- Every command in "How to run" is correct for this repo (check `build.gradle.kts`, `package.json`, `vite.config.js`, ports in config). If practical, run the automated tests and report their real result.
- Every term in the glossary is actually used in the document, and every jargon term used is in the glossary.
- Mermaid blocks are syntactically valid.

Finish by telling the user where the file is, how long it is (sections / approx. length), **which bugs you found and fixed** (with the files changed and tests added), and anything you flagged as missing, limited or unverified. Fixes are left uncommitted unless the user asks you to commit.
