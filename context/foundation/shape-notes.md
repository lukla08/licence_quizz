---
project: "ClickUp Simplifier"
context_type: greenfield
created: 2026-06-06
updated: 2026-06-06
product_type: "desktop + web-app (multi-client; specific client choice deferred)"
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 6
  hard_deadline: null
  after_hours_only: false
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "pain category"
      decision: "workflow friction + cognitive overload + data hard to access"
    - topic: "core insight"
      decision: "local copy of data + fast keyboard-first UI beats heavy online client"
    - topic: "primary persona scope"
      decision: "single user (the author); personal tool"
    - topic: "app access model"
      decision: "no auth; single user, single device, local data"
    - topic: "ClickUp connection auth"
      decision: "personal API token pasted by the user (no OAuth for MVP)"
    - topic: "MVP write scope"
      decision: "selective writes (milestone create + task->milestone assignment) first; edit surface grows over time"
    - topic: "MVP data scope"
      decision: "whole workspace, via tiered sync (dictionaries rare, tasks incremental/frequent, both manually triggerable)"
    - topic: "core domain model"
      decision: "milestones and tasks are never shown at the same level; a task belongs to a milestone"
    - topic: "timeline"
      decision: "~6-week MVP, longer-than-default cost explicitly accepted"
    - topic: "domain rule core"
      decision: "strict milestone->task hierarchy is the core rule; release note is a derived secondary feature"
    - topic: "offline scope"
      decision: "MVP requires a live ClickUp connection; offline operation is a non-goal"
    - topic: "data freshness"
      decision: "task changes reflected within minutes via frequent auto-sync (connection available)"
  frs_drafted: 14
  quality_check_status: accepted
---

# Shape Notes

> Seed idea (verbatim, PL): Od dłuższego czasu pracuję z ClickUp, interfejs jest
> „przeładowany", wielokrotnie okazał się nieprzewidywalny, brakuje mi spójnej i
> pewnej obsługi klawiatury. Bardziej odpowiadał mi interfejs Mantis. Zamierzam
> napisać klienta, który usunie wrażenie przeładowania, będzie pracował na kopii
> danych ClickUp. Synchronizacja danych będzie musiała być elementem rozwiązania.
> Nie zależy mi na wszystkich rozbudowanych funkcjach — zależy mi na małym
> zakresie obsłużonym intuicyjnie (zakres może się rozbudowywać).

## Vision & Problem Statement

A long-time ClickUp power user experiences the ClickUp web client as overloaded
and unpredictable. Routine task work takes too many clicks, there is no reliable
keyboard-driven flow, and the cluttered interface imposes a constant cognitive
load. The user also finds their own data hard to access and work with on their
own terms. The net cost: daily task management is slower than it should be, and
the tool is not trusted to behave consistently.

The insight: working on a *local copy* of ClickUp data behind a fast,
keyboard-first interface beats the heavy online client. A small, consistent,
predictable set of operations — closer in spirit to the Mantis interface the
user prefers — serves a power user better than ClickUp's full feature surface.
Synchronization between the local copy and ClickUp is a required part of the
solution, not an afterthought.

> Open thread: "unpredictable" is not yet decomposed into specific failures
> (focus jumps, inconsistent shortcuts, surprising save behavior). To be pinned
> down as guardrails in Phase 3.

## User & Persona

**Primary persona — the keyboard-driven power user (the author).** A single
person who manages tasks in ClickUp every day, works primarily from the
keyboard, and values predictability and speed over feature breadth. They have
used ClickUp long enough to know exactly which small set of operations they
actually rely on, and they preferred the leaner, more predictable Mantis-style
interface. This is a personal, single-user tool; multi-user scope is explicitly
out for now (may grow later).

## Access Control

Single user; no app-level authentication. The client runs locally for one
person on one device, and the local copy of the data lives on-device. There are
no roles and no access separation — a flat, single-user model.

The product does, however, hold one external credential: a **ClickUp personal
API token** that the user pastes into the app's settings. The app uses this
token to read from and synchronize with ClickUp on the user's behalf. OAuth is
explicitly out of scope for the MVP (it only earns its cost in a multi-user
product). The token is the only secret the app manages; how it is stored
securely on-device is a downstream implementation concern.

## Success Criteria

### Primary
- The client pulls the whole ClickUp workspace into a local copy via *tiered
  sync* — dictionaries (lists, folders) synced rarely, tasks synced
  incrementally and more frequently, and every sync action additionally
  triggerable on demand — and presents tasks strictly nested under milestones
  (milestones and tasks are never shown at the same level), fully
  keyboard-navigable.
- Working with tasks is context-first: the user selects a higher-level context
  (folder, etc.) first, then works on the tasks within it. The client remembers
  the previous selections, so the next launch resumes at the last chosen
  context.
- From the keyboard, the user can create a milestone and assign tasks to it, and
  those selected changes round-trip back to ClickUp.

### Secondary
- Access to a milestone's release note (exact definition still open — see Open
  Questions).
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

## Timeline acknowledgment

Acknowledged on 2026-06-06: ~6-week MVP (per single client) is longer than the
3-week default and requires sustained dedication; user accepted the cost
explicitly. Work happens within the day job (not after-hours).

## Functional Requirements

### Sync & local copy
- FR-001: User can store a ClickUp personal API token in settings. Priority: must-have
  > Socrates: No counter-argument accepted; stands as written (personal single-user tool; secure on-device token storage is a downstream concern).
- FR-002: User can pull the whole workspace into a local copy. Priority: must-have
  > Socrates: Counter considered: "whole-workspace pull is slow / hits API limits." Resolution: the initial copy is built once, then kept current by applying incremental changes — the full-pull cost is one-time, not recurring.
- FR-003: System syncs dictionaries (lists, folders) on a slower cadence and tasks incrementally on a faster cadence. Priority: must-have
  > Socrates: No counter-argument; the differing cadences ARE the mechanism that maintains the local copy (dictionaries change rarely, tasks often).
- FR-004: User can manually trigger a sync (dictionaries and/or tasks). Priority: nice-to-have
  > Socrates: No counter-argument; a cheap "refresh now" control gives confidence in freshness. Correctly nice-to-have.

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

## Business Logic

The app presents every task strictly within a milestone — milestones and tasks
are never peers — so the user always works in a two-level milestone→task
structure mirrored to and from ClickUp.

The rule consumes the milestones and tasks held in the local copy of the
workspace. Its output is a strictly two-level organization: within a chosen
higher-level context, the user sees milestones, and each task appears nested
under exactly one milestone (never beside one). The user encounters the rule
throughout the flow — selecting a context, navigating milestones and their
tasks from the keyboard, creating milestones, and assigning tasks to them — and
the resulting structure is propagated back to ClickUp through the reviewed
write-back (FR-014).

Release notes are a *derived, secondary* feature layered on top of this rule —
not part of the core rule itself: for a given milestone (or aggregated across
the current context's milestones), the app composes the list of that milestone's
resolved tasks (FR-011).

> Unresolved: how tasks with no milestone are presented in this strict two-level
> structure — see Open Questions #2.

## Non-Functional Requirements

- Navigating and reading the local copy presents results within ~100 ms as the
  user perceives it, even at whole-workspace size.
- Every keyboard action produces the same result on every run, and focus never
  moves except as the direct result of a user action (no surprise focus jumps).
- No local change is ever lost during synchronization; changes reach ClickUp
  only after the user has explicitly reviewed and approved them.
- With a connection available, task changes made in ClickUp are reflected in the
  local copy within minutes under normal use (frequent automatic sync).

> MVP requires a live connection to ClickUp to function — offline operation is a
> non-goal (formalized in Non-Goals). The local copy exists for speed and a lean
> UI, not for disconnected use.

## Non-Goals

- **Multi-user, sharing, roles** — the tool is single-user; no shared accounts,
  permissions, or collaboration. (May grow later.)
- **Recreating ClickUp's feature richness** — comments, attachments, dashboards,
  automations, Gantt, custom views, etc. are deliberately out; the value is a
  lean, predictable subset, not parity.
- **Dictionary structure management** — creating or editing spaces/folders/lists
  from the client is out; dictionaries are synced read-only, not managed here.
- **Offline operation** — the MVP requires a live ClickUp connection; the local
  copy is for speed and a lean UI, not for disconnected use. (Non-functional
  non-goal.)
- **OAuth** — only a personal API token is supported for MVP; full OAuth is out
  until/unless the tool goes multi-user. (Non-functional non-goal.)

> Not locked: sub-tasks / levels deeper than milestone→task were deliberately
> NOT ruled out — the two-level model is the MVP presentation, but deeper nesting
> may be added as scope grows.

## Forward: tech-stack

(Not part of the PRD schema — captured for `/10x-tech-stack-selector`.)

- Client/stack choice is **deliberately deferred**. Exploration starts from
  several implementations in parallel, possibly all carried to completion:
  - native **web**
  - **Flutter** (emphasis on web and desktop)
  - native **desktop in Java**
- Architectural implication to carry forward: a **shared core** (local copy +
  tiered sync + milestone→task domain model + reviewed write-back) with
  **swappable front-ends**, so multiple clients can sit on one engine.
- ClickUp integration uses a **personal API token** (no OAuth) for MVP.

## Open Questions

1. ~~**What exactly is a milestone "release note"?**~~ **RESOLVED 2026-06-06:** a
   read-only list of resolved (completed) tasks in a milestone, or aggregated
   across all milestones in the current context. Captured in FR-011.
2. **How are tasks not assigned to any milestone presented?** — Proposed: a
   virtual "no milestone" node so they stay visible (strict hierarchy must not
   hide them). Owner: user. To pin down in Business Logic (Phase 5).
3. **What is the write-back review mechanism?** — From FR-014: changes push to
   ClickUp only after explicit user review/approval. Exact UX (e.g. a pending-
   changes queue + confirm step) to be specified. Owner: user / downstream.
