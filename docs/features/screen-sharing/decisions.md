# Screen Sharing Testing - Decisions

## Key Decisions

### Decision 1: Fake Media Stream for Screen Share

**Context:** Containerized browsers cannot access actual screen content for sharing.

**Decision:** Use Chrome/Firefox fake media stream capabilities to simulate screen share.

**Rationale:**
- Chrome supports `--use-fake-device-for-media-stream` which generates synthetic video
- Firefox supports `media.navigator.streams.fake` preference
- This approach aligns with existing camera/microphone testing patterns
- No OS-level dependencies required

**Alternatives Considered:**
- Pre-recorded video file injection (more complex, unnecessary for track verification)
- Headless browser with virtual display (introduces additional dependencies)

**Consequences:**
- Screen share "content" is a generated pattern, not actual screen content
- Track publishing and subscription can still be verified via server API
- Visual verification via VNC recording shows synthetic video

---

### Decision 2: Server-Side Track Verification

**Context:** Need to verify screen share tracks are properly published and visible to subscribers.

**Decision:** Use LiveKit RoomServiceClient API to verify track presence and type.

**Rationale:**
- Server API provides definitive source of truth for track state
- TrackInfo includes `source` field to identify screen share vs camera
- Aligns with existing pattern in `LiveKitRoomSteps.java`
- More reliable than client-side DOM inspection

**Alternatives Considered:**
- JavaScript-based track inspection in browser (less reliable, may miss server-side issues)
- WebRTC stats inspection (more complex, overkill for basic verification)

**Consequences:**
- Tests verify server-side state, not client rendering
- Track type identification relies on LiveKit's source enumeration
- Polling may be needed for track publication timing

---

### Decision 3: Permission Grant via canPublishSources

**Context:** Need to control which participants can share screens.

**Decision:** Use `canPublishSources` grant with specific source types.

**Rationale:**
- LiveKit's recommended approach for source-level permission control
- More granular than simple `canPublish` boolean
- Enables negative testing (camera allowed, screen share denied)

**Alternatives Considered:**
- Using `canPublish` alone (doesn't allow source-specific control)
- Room-level configuration (affects all participants, less flexible for testing)

**Consequences:**
- AccessTokenStateManager may need extension for `canPublishSources` parsing
- Grant format in feature files: `canPublishSources:screen_share,canPublishSources:camera`
- Minimum LiveKit version requirement for full support

---

### Decision 4: Story Breakdown by Track Lifecycle

**Context:** Original story covered multiple aspects of screen sharing.

**Decision:** Split into 5 sub-stories following track lifecycle (permission -> publish -> subscribe -> stop -> denied).

**Rationale:**
- Each story is independently testable and deliverable
- Follows INVEST criteria (Independent, Negotiable, Valuable, Estimable, Small, Testable)
- Allows incremental delivery and early feedback
- Dependencies flow naturally from lifecycle

**Alternatives Considered:**
- Single large story (violates "Small" principle)
- Split by browser type (introduces test duplication)
- Split by participant role (less cohesive)

**Consequences:**
- 5 stories instead of 1, each XS or S sized
- Clear implementation order based on dependencies
- Can demo progress at each story completion

---

### Decision 5: Browser Coverage - Chrome and Firefox

**Context:** Need to verify screen sharing works across browsers.

**Decision:** Test screen sharing on Chrome and Firefox; defer Edge testing.

**Rationale:**
- Chrome is primary browser with best WebRTC support
- Firefox provides cross-browser verification
- Edge uses Chromium engine, likely behaves same as Chrome
- Reduces test execution time

**Alternatives Considered:**
- Test all three browsers (increases execution time by 50%)
- Test only Chrome (misses Firefox-specific issues)

**Consequences:**
- Scenario Outline for cross-browser testing includes Chrome and Firefox
- Edge coverage can be added later if needed
- Browser-specific issues may surface only on untested browsers

---

## Open Decisions

### Pending: Screen Share with Audio

**Question:** Should we include `screen_share_audio` track testing in this story?

**Options:**
1. Include in current story (increases scope)
2. Defer to separate story (keeps current story focused)

**Recommendation:** Defer to separate story. System audio capture adds complexity and may not work in containerized environments.

---

### Pending: Minimum LiveKit Version

**Question:** What is the minimum LiveKit version that fully supports `canPublishSources`?

**Action Required:** Verify against older versions during implementation.

**Default:** Target v1.5.0+ until verified.

---

### Pending: Web Application UI Design

**Question:** How should the screen share button be styled and positioned in meet.html?

**Options:**
1. Add to existing control bar (consistent with camera/mute)
2. Separate screen share section (more prominent)

**Recommendation:** Add to existing control bar for consistency with current UI pattern.
