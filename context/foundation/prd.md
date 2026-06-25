---
project: "ClickUp Simplifier"
version: 1
status: draft
created: 2026-06-06
context_type: greenfield
product_type: "desktop app (JavaFX monolith; multi-client + server split deferred post-MVP — rev. 2026-06-25)"
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 6
  hard_deadline: null
  after_hours_only: false
---

# PRD: ClickUp Simplifier

## Vision & Problem Statement

A long-time ClickUp power user experiences the ClickUp web client as overloaded
and unpredictable. Routine task work takes too many clicks, there is no reliable
keyboard-driven flow, and the cluttered interface imposes a constant cognitive
load. The user also finds their own data hard to access and work with on their
own terms. The net cost: daily task management is slower than it should be, and
the tool is not trusted to behave consistently.

The insight: working on a *local copy* of ClickUp data behind a fast,
keyboard-first interface beats the heavy online client. A small, consistent,
predictable set of operations — closer in spirit to the leaner Mantis interface
the user prefers — serves a power user better than ClickUp's full feature
surface. Synchronization between the local copy and ClickUp is a required part of
the solution, not an afterthought.

## User & Persona

**Primary persona — the keyboard-driven power user (the author).** A single
person who manages tasks in ClickUp every day, works primarily from the
keyboard, and values predictability and speed over feature breadth. They have
used ClickUp long enough to know exactly which small set of operations they
actually rely on, and they preferred the leaner, more predictable Mantis-style
interface. This is a personal, single-user tool; multi-user scope is explicitly
out for now (may grow later).

## Success Criteria

### Primary
- The client pulls the whole ClickUp workspace into a local copy via *tiered
  sync* organized as **named sync sets** — "Podstawowe słowniki" (dictionaries:
  lists, folders) synced rarely and "Zadania" (tasks) synced incrementally and
  more frequently — and presents tasks strictly nested under milestones
  (milestones and tasks are never shown at the same level), fully
  keyboard-navigable.
- Working with tasks is context-first: the user selects a higher-level context
  (folder, etc.) first, then works on the tasks within it. The client remembers
  the previous selections, so the next launch resumes at the last chosen context.
- From the keyboard, the user can create a milestone and assign tasks to it, and
  those selected changes round-trip back to ClickUp.
- A **sync management panel** gives the user full visibility and control over
  synchronization: per sync set it shows the last successful and last failed sync
  time (with the error description on failure), lets the user trigger a sync
  immediately, and lets the user set each set's automatic frequency.

### Secondary
- Access to a milestone's release note — a read-only list of resolved tasks per
  milestone, or aggregated across the current context's milestones.
- Consistent, predictable keyboard handling across the whole app.

### Guardrails
- Selected writes must round-trip to ClickUp without corrupting or duplicating
  existing data.
- A sync must never silently lose a local edit; conflict handling must at least
  be safe (no surprise overwrite).
- Keyboard behaviour must be consistent and predictable — no focus jumps, no
  shortcut that behaves differently run-to-run. (This is the original ClickUp
  pain; regressing here means the product failed at its core promise.)
- Navigating the local copy stays instant even at whole-workspace size.

## User Stories

### US-01: User organizes tasks under milestones in a fast keyboard-driven view

- **Given** a configured ClickUp token and a synced local copy of the workspace
- **When** the user selects a higher-level context, navigates with the keyboard,
  creates a milestone, and assigns tasks to it
- **Then** the tasks appear strictly nested under their milestone, and the
  selected changes are synchronized back to ClickUp

#### Acceptance Criteria
- Milestones and tasks are never rendered at the same level
- Every step is completable without the mouse
- Selected changes round-trip to ClickUp without creating duplicates
- (If FR-006 is built) the chosen context is restored on the next launch

### US-02: User manages synchronization and trusts the freshness of the local copy

- **Given** a configured ClickUp token and at least one sync set that has run before
- **When** the user opens the sync management panel
- **Then** they see, per sync set, the last successful and last failed sync time (with
  the error description on failure), can trigger any set immediately, and can set each
  set's automatic frequency from presets or a custom value

#### Acceptance Criteria
- Each sync set shows its last-success and last-failure time independently
- A failed sync shows a readable error description
- Immediate (on-demand) trigger works regardless of the set's schedule
- A changed frequency (preset or custom value) is persisted and takes effect

## Functional Requirements

### Sync & local copy
- FR-001: User can store a ClickUp personal API token in settings. Priority: must-have
  > Socrates: No counter-argument accepted; stands as written (personal single-user tool; secure on-device token storage is a downstream concern).
- FR-002: User can pull the whole workspace into a local copy. Priority: must-have
  > Socrates: Counter considered: "whole-workspace pull is slow / hits API limits." Resolution: the initial copy is built once, then kept current by applying incremental changes — the full-pull cost is one-time, not recurring.
- FR-003: System organizes synchronization into **named sync sets** — "Podstawowe słowniki" (dictionaries: lists, folders; slower cadence) and "Zadania" (tasks; faster, incremental cadence). Priority: must-have
  > Socrates: No counter-argument; the differing cadences ARE the mechanism that maintains the local copy (dictionaries change rarely, tasks often). Naming the sets makes them addressable in the management panel (FR-015..FR-019).
- FR-004: User can manually trigger a sync set on demand. Priority: must-have
  > Socrates: Originally nice-to-have; promoted to must-have because the sync management panel makes on-demand activation a first-class control (see FR-018), not an optional refresh. A cheap "sync now" gives confidence in freshness.

### Sync management panel
- FR-015: System provides a sync management panel that lists every named sync set. Priority: must-have
  > Socrates: No counter-argument; the panel is the single surface where the user observes and controls synchronization — central to trusting the local copy.
- FR-016: For each sync set, the panel shows the timestamp of the last successful sync and the timestamp of the last failed sync. Priority: must-have
  > Socrates: No counter-argument; "last OK" answers "is my copy fresh?" and "last error" answers "did something break?" — both are needed to trust the data.
- FR-017: When a sync set's last run failed, the panel shows the error description. Priority: must-have
  > Socrates: No counter-argument; a failure without a reason is not actionable. The description turns a silent failure into something the user can diagnose (e.g. expired token, API limit).
- FR-018: User can immediately activate (trigger now) any sync set from the panel. Priority: must-have
  > Socrates: Realizes FR-004 as a concrete panel control; the panel is where on-demand sync lives.
- FR-019: User can define the automatic sync frequency for each sync set by picking from predefined options (preset cadences), with the additional ability to enter a custom value manually. Priority: must-have
  > Socrates: Counter considered: "fixed cadences are simpler." Resolution: dictionaries and tasks change at very different rates and the user knows their own rhythm; per-set frequency lets them tune cost vs. freshness rather than accept one hardcoded cadence. Presets cover the common cases fast (one keystroke), and the manual entry escape hatch handles the rest without locking the user into the preset list.

### Navigation & context
- FR-005: User can select a higher-level context (folder, etc.) to scope what they work on. Priority: must-have
  > Socrates: No counter-argument; scoping to a context is what reduces the overload the product exists to fix.
- FR-006: System remembers the user's previous context selection and restores it on next launch. Priority: nice-to-have
  > Socrates: No counter-argument; resuming at the last context is real daily convenience. Correctly nice-to-have.
- FR-007: User can operate the entire interface from the keyboard. Priority: must-have
  > Socrates: No counter-argument; keyboard operation is the core promise — the exact pain that drove leaving ClickUp.
- FR-008: System presents tasks strictly nested under milestones (milestones and tasks never at the same level). Priority: must-have
  > Socrates: Counters considered: "may limit flexibility" and "where do tasks with no milestone go?" Resolution: stands — this is the central domain rule; unassigned tasks are shown under a virtual "no milestone" node so nothing disappears (to be pinned in Business Logic).

### Milestones
- FR-009: User can create a milestone. Priority: must-have
  > Socrates: No counter-argument; creating milestones is the backbone of the organization model.
- FR-010: User can assign a task to a milestone. Priority: must-have
  > Socrates: No counter-argument; this is the other half of the central rule — without it the hierarchy is empty.
- FR-011: User can view a milestone's release note — the list of resolved (completed) tasks in that milestone, or aggregated across all milestones in the current context. Priority: nice-to-have
  > Socrates: Definition clarified (a read-only list of resolved tasks per milestone, or aggregated across the context's milestones) composed from data already held; the "built before defined" counter no longer applies.

### Task editing
- FR-012: User can change a task's status / mark a task complete. Priority: must-have
  > Socrates: No counter-argument; status change is the most frequent daily operation.
- FR-013: User can edit a task's title and description. Priority: must-have
  > Socrates: No counter-argument; basic keyboard editing. Rich-text/conflict risks are handled by the write-back review (FR-014) and NFRs.

### Write-back
- FR-014: Selected changes (milestone creation, task→milestone assignment, task status, task title/description) round-trip back to ClickUp, pushed only after an explicit user review/approval (never silent auto-push). Priority: must-have
  > Socrates: Counter considered: "two-way write-back is the riskiest part." Resolution: changes are pushed to ClickUp only after the user explicitly reviews/approves them — safety over convenience. Exact review UX (queue + confirm) to be specified.

> Note: edit surface is expected to grow over later versions; FR-012/FR-013 are
> the first writes beyond milestone organization. More may be added.

## Non-Functional Requirements

- Navigating and reading the local copy presents results within ~100 ms as the
  user perceives it, even at whole-workspace size.
- Every keyboard action produces the same result on every run, and focus never
  moves except as the direct result of a user action (no surprise focus jumps).
- No local change is ever lost during synchronization; changes reach ClickUp
  only after the user has explicitly reviewed and approved them.
- With a connection available, task changes made in ClickUp are reflected in the
  local copy within minutes under normal use (frequent automatic sync).

## Business Logic

The app presents every task strictly within a milestone — milestones and tasks
are never peers — so the user always works in a two-level milestone→task
structure mirrored to and from ClickUp.

The rule consumes the milestones and tasks held in the local copy of the
workspace. Its output is a strictly two-level organization: within a chosen
higher-level context, the user sees milestones, and each task appears nested
under exactly one milestone (never beside one). The user encounters the rule
throughout the flow — selecting a context, navigating milestones and their tasks
from the keyboard, creating milestones, and assigning tasks to them — and the
resulting structure is propagated back to ClickUp through the reviewed write-back
(FR-014). Release notes are a *derived, secondary* feature layered on top of this
rule: for a given milestone (or aggregated across the current context's
milestones), the app composes the list of that milestone's resolved tasks
(FR-011).

Synchronization is modelled as a small fixed catalogue of **named sync sets**,
each with its own cadence and its own status: "Podstawowe słowniki"
(dictionaries, slow cadence) and "Zadania" (tasks, fast incremental cadence).
Each set independently tracks the time of its last successful run and the time of
its last failed run (a later failure does not erase the last-known-good time, and
vice versa), and a failed run records an error description. The user can activate
a set immediately, independent of its schedule, and set the frequency of its
automatic runs by picking a preset cadence or entering a custom value. The set
catalogue is fixed for the MVP; user-defined custom sync sets are out of scope.

## Access Control

Single user; no app-level authentication. The client runs locally for one person
on one device, and the local copy of the data lives on-device. There are no roles
and no access separation — a flat, single-user model.

The product holds one external credential: a **ClickUp personal API token** that
the user pastes into the app's settings. The app uses this token to read from and
synchronize with ClickUp on the user's behalf. OAuth is explicitly out of scope
for the MVP (it only earns its cost in a multi-user product). The token is the
only secret the app manages; how it is stored securely on-device is a downstream
implementation concern.

## Non-Goals

- **Multi-user, sharing, roles** — the tool is single-user; no shared accounts,
  permissions, or collaboration. (May grow later.)
- **Recreating ClickUp's feature richness** — comments, attachments, dashboards,
  automations, Gantt, custom views, etc. are deliberately out; the value is a
  lean, predictable subset, not parity.
- **Dictionary structure management** — creating or editing spaces/folders/lists
  from the client is out; dictionaries are synced read-only, not managed here.
- **User-defined custom sync sets** — the sync set catalogue is fixed for the MVP
  (two named sets); the user tunes each set's cadence but cannot define new sets.
- **Offline operation** — the MVP requires a live ClickUp connection; the local
  copy is for speed and a lean UI, not for disconnected use.
- **OAuth** — only a personal API token is supported for MVP; full OAuth is out
  until/unless the tool goes multi-user.
- **Multi-client + wydzielony serwer** (rev. 2026-06-25) — MVP to **monolit
  desktopowy pod JavaFX**: jeden klient (Flutter i web wypadają), brak serwera
  HTTP i kontraktu sieciowego (rdzeń wołany in-process). Granice dziedzin i
  warstwy pozostają wydzielone (moduły `core` + `ui`), więc powrót do
  rozbudowanej, wielo-klientowej / klient-serwer wersji jest możliwy później.

> Not locked: sub-tasks / levels deeper than milestone→task were deliberately
> NOT ruled out — the two-level model is the MVP presentation, but deeper nesting
> may be added as scope grows.

## Open Questions

1. **How are tasks not assigned to any milestone presented?** — Proposed: a
   virtual "no milestone" node so they stay visible (the strict hierarchy must
   not hide them). Owner: user. To pin down in Business Logic.
2. **What is the write-back review mechanism?** — From FR-014: changes push to
   ClickUp only after explicit user review/approval. Exact UX (e.g. a
   pending-changes queue + confirm step) to be specified. Owner: user /
   downstream.
3. **What are the predefined auto-sync frequency presets?** — FR-019 commits to
   presets + custom entry, but the concrete preset cadences (and sensible
   defaults per set) are not yet chosen. Owner: user.
4. **Does sync management warrant its own user story?** — ✅ Resolved (2026-06-20):
   yes. Added **US-02** (sync management) with Given/When/Then + acceptance criteria
   covering FR-015..FR-019. Owner: user / downstream.
