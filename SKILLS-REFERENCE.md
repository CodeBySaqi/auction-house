# 🏆 Three Skills Applied to Auction House Project

## Overview
These are three powerful coding-agent skills adapted for our Java Spring Boot project.
Each serves a different phase of development:

| Skill | Phase | Purpose |
|-------|-------|---------|
| **Blueprint** | 📋 Planning | Plan features properly before writing code |
| **TDD** | 🧪 Testing | Write tests first, then code, then refactor |
| **VibeSec** | 🔒 Security | Audit code from a bug-hunter's perspective |

---

## 1. 📋 BLUEPRINT — Planning Copilot

**Source:** [imbue-ai/blueprint](https://github.com/imbue-ai/blueprint)

### How It Works
1. **Parse** — Understand the feature description
2. **Explore** — Read the actual codebase (files, patterns, architecture)
3. **Question** — Ask 3-5 clarifying questions per round
4. **Refine** — Build a refined prompt incorporating answers
5. **Generate** — Produce a markdown plan any agent can execute

### The Blueprint Workflow
```
✓ Explore  ● Plan  ○ Write  ○ Refine
```

### When to Use
- Before implementing any NEW feature
- Before large refactors
- When requirements are ambiguous
- When planning touches multiple systems

### Blueprint Rules
- Gather facts BEFORE asking questions
- Don't ask questions whose answers are obvious from the code
- Questions must be grounded in actual codebase exploration
- Only the user decides when Q&A is done
- Never write code during planning — only gather information

### Applied to Your Project
Use Blueprint methodology for:
- Notification centralization system (Bug #11 we skipped)
- Email notification system
- WebSocket live bid updates
- Payment gateway integration
- Mobile app API design

---

## 2. 🧪 TEST-DRIVEN DEVELOPMENT (TDD)

**Source:** [obra/superpowers/test-driven-development](https://github.com/obra/superpowers/tree/main/skills/test-driven-development)

### The Iron Law
```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

### Red-Green-Refactor Cycle
```
RED (write failing test) → Verify fails → GREEN (minimal code) → Verify passes → REFACTOR → Repeat
```

### When to Use
- **Always** for new features
- **Always** for bug fixes
- **Always** for refactoring
- **Always** for behavior changes

### TDD Rules
1. Write ONE minimal test showing what should happen
2. WATCH IT FAIL (mandatory — never skip)
3. Write simplest code to pass
4. WATCH IT PASS (mandatory)
5. Refactor only while green
6. Repeat for next behavior

### Good Test Characteristics
| Quality | Good | Bad |
|---------|------|-----|
| **Minimal** | One behavior | `test('validates email and domain')` |
| **Clear name** | Describes behavior | `test('test1')` |
| **Shows intent** | Demonstrates desired API | Obscures what code does |
| **Real code** | Tests actual behavior | Tests mocks |

### Common Excuses (and why they're wrong)
| Excuse | Reality |
|--------|---------|
| "Too simple to test" | Simple code breaks. Test takes 30 seconds. |
| "I'll test after" | Tests-after pass immediately — proves nothing |
| "Already manually tested" | Manual = ad-hoc, no record, can't re-run |
| "TDD will slow me down" | TDD catches bugs before commit, prevents regressions |

### Red Flags — STOP and Start Over
- Code written before test
- Test passes immediately (testing existing behavior)
- Can't explain why test failed
- Rationalizing "just this once"

### Applied to Your Project
**Tests we should write for the bugs we fixed:**

```java
// Bug #2: Double-cancel prevention
@Test
void cancelAuction_shouldThrowIfAlreadyCancelled() { ... }

// Bug #3: Reopen ACTIVE auction prevention
@Test
void bulkReopen_shouldSkipAlreadyActiveAuctions() { ... }

// Bug #4: Negative balance prevention
@Test
void adjustBalance_shouldRejectNegativeResult() { ... }

// Bug #13: Past end times
@Test
void demoSeeder_activeAuctionsShouldHaveFutureEndTimes() { ... }

// Bug #6: Bid consecutive check
@Test
void placeBid_shouldRejectConsecutiveBidsBySameUser() { ... }
```

---

## 3. 🔒 VIBESEC — Security Audit Skill

**Source:** [BehiSecc/VibeSec-Skill](https://github.com/BehiSecc/VibeSec-Skill)

### Core Principles
1. **Defense in depth** — Never rely on a single security control
2. **Fail securely** — When something fails, fail closed (deny access)
3. **Least privilege** — Grant minimum permissions necessary
4. **Input validation** — Never trust user input, validate server-side
5. **Output encoding** — Encode data for the context it's rendered in

### Security Areas to Audit

#### A. Access Control
- [ ] Verify user owns resource on every request
- [ ] Check role permissions for role-based actions
- [ ] Validate parent resource ownership
- [ ] Use UUIDs instead of sequential IDs
- [ ] Re-validate permissions after privilege changes

#### B. XSS Protection
- [ ] All user input encoded when rendered in HTML
- [ ] CSP headers set
- [ ] SVG uploads sanitized or blocked
- [ ] Rich text whitelisted (not blacklist)
- [ ] Error messages don't reflect user input

#### C. CSRF Protection
- [ ] All POST/PUT/PATCH/DELETE have CSRF tokens
- [ ] Tokens tied to user session
- [ ] Missing token = rejected request
- [ ] SameSite cookies set
- [ ] Tokens regenerated on auth state change

#### D. File Upload Security
- [ ] File type validated by extension AND magic bytes
- [ ] Files renamed to random UUIDs
- [ ] Files stored outside webroot
- [ ] Served with correct Content-Type
- [ ] Size limits enforced server-side

#### E. Path Traversal
- [ ] Never use user input directly in file paths
- [ ] Paths canonicalized and validated against base directory
- [ ] File extensions restricted

#### F. SQL Injection
- [ ] All queries use parameterized statements
- [ ] ORDER BY uses whitelisted values only
- [ ] Error messages don't expose SQL

#### G. Secret Exposure
- [ ] No API keys in client-side code
- [ ] No secrets in JS bundles or source maps
- [ ] Environment variables not exposed via build tools
- [ ] Debug info disabled in production

#### H. Open Redirect
- [ ] Redirect URLs validated against allowlist
- [ ] Only relative paths accepted
- [ ] No user input in redirect targets

#### I. Mass Assignment
- [ ] Only whitelisted fields accepted in request bodies
- [ ] Role fields never assignable by regular users
- [ ] Sensitive fields filtered server-side

### Applied to Your Project — Audit Findings

**Already Fixed:**
- ✅ CSRF on money-moving routes (Bug #4)
- ✅ Path traversal in FileStorageService (Bug #3)
- ✅ Exception message leaking in GlobalExceptionHandler (Bug #10)
- ✅ CSV formula injection (Bug #13)
- ✅ ROLE_BANNED bypass (Bug #3)
- ✅ Privilege escalation prevention (ADMIN → SUPER_ADMIN blocked)

**Needs Attention:**
- ⚠️ Sequential IDs used (not UUIDs) — IDOR risk if authorization checks are missed
- ⚠️ No CSP headers configured
- ⚠️ `imageUrl` field accepts arbitrary URLs (potential SSRF if fetched server-side)
- ⚠️ Profile pic URL can be set to external URL (no validation)
- ⚠️ `adjustBalance` has no upper limit (could set absurd balances)
- ⚠️ Auction `description` field — check if HTML is escaped on render
- ⚠️ No rate limiting on login/register endpoints
- ⚠️ Session management — check timeout and concurrent session handling

---

## 🚀 How to Use These Going Forward

### For New Features → Use Blueprint
Say: *"Use Blueprint to plan [feature name]"*
I'll explore the codebase, ask clarifying questions, and produce a plan.

### For Bug Fixes → Use TDD
Say: *"Use TDD to fix [bug description]"*
I'll write a failing test first, watch it fail, implement the fix, verify.

### For Security Reviews → Use VibeSec
Say: *"Run a VibeSec audit on [file/feature]"*
I'll check every security control against the checklist above.

### Combined Workflow
```
1. Blueprint → Plan the feature
2. TDD      → Write tests, implement, refactor
3. VibeSec  → Audit the implementation for security
4. Commit   → Atomic commit with clear message
5. Push     → Ship it
```
