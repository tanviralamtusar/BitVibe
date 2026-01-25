# Product Requirements Document (PRD)
## Advanced Music Player for Android

---

## 1. Executive Summary

### 1.1 Product Overview
A feature-rich Android music player designed for musicians, music students, and audio enthusiasts who need precision playback controls. The app differentiates itself through advanced A-B looping and pitch-preserved speed control, enabling users to practice instruments, learn songs, and analyze music with professional-grade tools.

### 1.2 Target Users
- Musicians and music students practicing instruments
- Audio learners studying songs or podcasts
- DJs and audio professionals
- General music enthusiasts who want advanced playback control

### 1.3 Success Metrics
- 100,000+ downloads in the first 6 months
- 4.5+ star rating on Google Play Store
- Daily Active Users (DAU) retention rate above 40%
- Average session duration of 25+ minutes
- Less than 2% crash rate

---

## 2. Core Features & Requirements

### 2.1 Must-Have Precision Tools (Priority 1)

#### 2.1.1 A-B Repeat (Segment Loop)
**Description:** Allow users to mark and continuously loop a specific segment of audio.

**Functional Requirements:**
- **Button A:** Taps to set the loop start point at the current playback position
- **Button B:** Taps to set the loop end point at the current playback position
- **Loop Toggle:** Enable/disable loop playback with visual indicator (active/inactive state)
- **Fine-Tune Controls:**
  - "<" button: Shift the active marker (A or B) backward by 100ms
  - ">" button: Shift the active marker forward by 100ms
  - Long-press: Adjust by 10ms increments for ultra-precision
- **Visual Feedback:**
  - Highlighted region on waveform/seek bar showing A-B segment
  - Timestamp display showing A and B positions (MM:SS.mmm format)
  - Active marker indicator (A or B selected for fine-tuning)

**Technical Requirements:**
- Loop boundary precision: ±5ms accuracy
- Instant loop restart (no audio gap or click)
- Persist loop points when switching between Normal/Pro Mode
- Save loop points per song in local database

**User Flow:**
1. User plays a song
2. Taps "A" button at desired loop start → Visual marker appears
3. Taps "B" button at desired loop end → Visual segment highlighted
4. Taps "Loop" toggle → Music loops between A-B continuously
5. Uses < > buttons to fine-tune exact boundaries
6. Taps "Loop" toggle again to disable and resume normal playback

#### 2.1.2 Precision Speed Control
**Description:** Adjust playback speed while preserving natural pitch quality.

**Functional Requirements:**
- **Speed Slider:**
  - Range: 0.25x to 2.0x
  - Step increment: 0.05x
  - Real-time adjustment (no playback interruption)
- **Pitch Preservation:**
  - Must use time-stretching algorithm (WSOLA, PSOLA, or similar)
  - Audio must sound natural at all speeds (no "chipmunk" effect when sped up, no "demonic" effect when slowed down)
- **Quick Toggle Presets:**
  - 0.5x (Half Speed)
  - 0.75x (Practice Speed)
  - 1.0x (Normal) - resets to original speed
  - 1.25x (Fast Forward)
- **Visual Feedback:**
  - Current speed displayed as percentage (e.g., "75%")
  - Slider position matches current speed
  - Color-coded indicator: Green (1.0x), Blue (<1.0x), Orange (>1.0x)

**Technical Requirements:**
- Algorithm: Implement Sonic library or SoundTouch for Android
- Latency: Speed changes apply within 100ms
- Audio quality: Minimal artifacts at all speed ranges
- CPU efficiency: Should not drain battery excessively

**Accessibility Requirement:**
- Speed controls must be accessible from the Now Playing screen without navigating to settings
- Available in both Normal and Pro Mode (persistent bottom sheet or toggle panel)

---

### 2.2 Core Playback Features (Priority 1)

#### 2.2.1 Standard Playback Controls
- Play/Pause button with loading state
- Next/Previous track buttons
- 10-second skip forward/backward buttons (optional secondary control)
- Volume control integration with system volume

#### 2.2.2 Seek Bar
- Drag to any position in the track
- Display current time and total duration
- Visual progress indicator
- Touch responsiveness: Update position within 50ms

#### 2.2.3 Playback Modes
- **Shuffle:** Randomize playback order within current queue
- **Repeat All:** Loop the entire queue/playlist
- **Repeat One:** Loop the current track infinitely
- Visual indicators for active modes

#### 2.2.4 Advanced Playback
- **Gapless Playback:** Zero silence between consecutive tracks (critical for live albums, DJ mixes)
- **Crossfade:** Configurable fade duration (0-12 seconds)
  - Fade out current track while fading in next track
  - Settings option to enable/disable and adjust duration
- **Background Play:** Continue playback when app is minimized or screen is off
  - Foreground service with persistent notification
  - Wake lock management for battery optimization

**Technical Requirements:**
- MediaPlayer or ExoPlayer framework
- Foreground service for background playback
- Audio focus handling (pause when calls arrive, duck for notifications)

---

### 2.3 Library & Organization (Priority 1)

#### 2.3.1 Folder View
- Display file system structure exactly as stored on device
- Support for Internal Storage and External SD Card
- Navigate through folders like a file manager
- Play entire folder or individual files

#### 2.3.2 Smart Categorization
Auto-organize music library by:
- **Songs:** Alphabetical list of all tracks
- **Artists:** Group by artist name, show song count
- **Albums:** Group by album name, show album art grid
- **Genres:** Auto-detect from metadata tags

#### 2.3.3 Playlist Management
- Create new playlists with custom names
- Add/remove songs via long-press or drag-and-drop
- Reorder songs within playlist (drag handles)
- Delete playlists with confirmation dialog
- Export playlist as .m3u file
- Import external playlists

#### 2.3.4 Favorites System
- Heart icon on Now Playing screen
- "Favorites" playlist auto-created
- Quick toggle (tap to add/remove)

#### 2.3.5 Search
- Real-time search with autocomplete
- Search by song title, artist, album, or filename
- Display results categorized by type
- Search history (last 10 searches)

#### 2.3.6 Folder Exclusion
- Settings option to exclude specific folders
- Default exclusions: WhatsApp Audio, Ringtones, Notifications
- User can add custom exclusion paths

**Technical Requirements:**
- MediaStore API for music scanning
- SQLite database for playlists and metadata
- Content provider for efficient queries
- Background scanning with progress notification

---

### 2.4 Audio Engine (Priority 2)

#### 2.4.1 Format Support
**Required Formats:**
- MP3 (MPEG-1/2 Audio Layer 3)
- AAC (Advanced Audio Coding)
- FLAC (Free Lossless Audio Codec)
- WAV (Waveform Audio File Format)
- M4A (MPEG-4 Audio)
- OGG (Ogg Vorbis)

**Technical Requirements:**
- Use Android's MediaCodec or ExoPlayer for broad format support
- Display codec information in song details

#### 2.4.2 Equalizer (EQ)
- **10-band graphic equalizer**
  - Frequency bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
  - Gain range: -12dB to +12dB per band
- **Presets:**
  - Flat (no adjustment)
  - Rock, Pop, Jazz, Classical, Hip-Hop, Electronic, Vocal Booster
- **Custom Presets:**
  - Allow users to save their own EQ configurations
  - Name and manage custom presets
- Visual: Real-time frequency visualization (spectrum analyzer)

#### 2.4.3 Additional Audio Effects
- **Bass Boost:** 0-100% slider, enhances low frequencies (60-250Hz)
- **Virtualizer:** 0-100% slider, simulates surround sound
- **Reverb:** Optional room effect (Small Room, Medium Room, Large Hall, etc.)
- **Mono Audio:** Toggle to merge left/right channels (accessibility feature)

**Technical Requirements:**
- Use Android's AudioEffect API
- Effects should work simultaneously without conflicts
- Save effect settings per-user (global) or per-song (advanced feature)

---

### 2.5 UI & User Experience (Priority 2)

#### 2.5.1 Lyrics Support
- **Offline Lyrics:**
  - Read embedded ID3 tags (USLT frame)
  - Read external .lrc files (same filename as audio file)
- **Synced Lyrics:**
  - Parse LRC timestamps (e.g., [00:12.50] Line of lyrics)
  - Highlight current line based on playback position
  - Auto-scroll to follow playback
- **Manual Editing:**
  - Allow users to paste/edit lyrics
  - Save to database or external .lrc file
- **Fallback:** Display "No lyrics available" if not found

#### 2.5.2 Sleep Timer
- Preset durations: 5, 10, 15, 30, 45, 60 minutes
- Custom duration picker (up to 120 minutes)
- "End of Track" option (stop after current song finishes)
- Countdown display in notification
- Gradual fade-out (optional, last 10 seconds)

#### 2.5.3 Themes
- **Light Mode:** White/light gray backgrounds
- **Dark Mode:** Dark gray backgrounds, follows system theme
- **AMOLED Black:** Pure black (#000000) for battery saving on OLED screens
- Accent color customization (choose from palette)
- Auto-switch based on time of day (optional)

#### 2.5.4 Album Art Editor
- **Auto-fetch:** Download album art from online sources (Last.fm, MusicBrainz)
- **Manual Upload:** Pick image from gallery or camera
- **Crop & Resize:** Built-in editor for square album art
- **Embed in File:** Option to save art directly to audio file metadata

#### 2.5.5 Waveform View
- **Visual Representation:** Display audio waveform instead of simple progress bar
- **Zoom Levels:** Pinch-to-zoom for detailed view
- **A-B Loop Integration:** Waveform highlights looped segment in color
- **Beat Detection:** Optional visual markers for beats/bars (advanced feature)
- **Performance:** Generate waveform in background thread, cache for reuse

**Technical Requirements:**
- Use FFmpeg or Ringdroid's WaveformView library
- Render waveform as scalable vector or bitmap
- Lazy loading for large files (>10MB)

---

### 2.6 System Integration (Priority 1)

#### 2.6.1 Notification Control
- **Persistent Notification:**
  - Song title, artist, album art
  - Play/Pause, Previous, Next buttons
  - Swipe-to-dismiss stops playback
- **Lock Screen Media Controls:**
  - Full-screen album art (optional)
  - Same controls as notification
- **Android Auto / Wear OS (Future):**
  - Basic playback control on car displays and smartwatches

#### 2.6.2 Headset Support
- **Button Controls:**
  - Single Click: Play/Pause
  - Double Click: Next Track
  - Triple Click: Previous Track
  - Long Press: Optional (activate voice assistant or custom action)
- **Auto-Play on Connect:**
  - Settings toggle to auto-resume playback when headphones are plugged in or Bluetooth connects
- **Auto-Pause on Disconnect:**
  - Pause immediately when headphones are unplugged or Bluetooth disconnects

**Technical Requirements:**
- BroadcastReceiver for headset button events
- BluetoothAdapter for wireless headset detection

---

## 3. UI/UX Design Specifications

### 3.1 Main Screen Layout - Two Modes

#### 3.1.1 Normal Mode (Default)
**Purpose:** Clean, distraction-free listening experience.

**Layout:**
```
┌─────────────────────────────┐
│     [Mini Player Bar]       │ ← Persistent at bottom of all screens
├─────────────────────────────┤
│                             │
│    [Album Art - Large]      │ ← Dominates the screen
│         (Square)            │
│                             │
├─────────────────────────────┤
│   Song Title (Bold, 18sp)   │
│   Artist Name (14sp)        │
├─────────────────────────────┤
│   [♥ Favorite] [⋮ Menu]     │
├─────────────────────────────┤
│   ━━━━●━━━━━━━━━━━━━       │ ← Seek Bar
│   1:23        -2:15         │
├─────────────────────────────┤
│   [🔀] [⏮] [⏯] [⏭] [🔁]    │ ← Playback Controls
└─────────────────────────────┘
```

**Elements:**
- Album art occupies 60% of screen height
- Seek bar with current/total time
- Standard controls: Shuffle, Previous, Play/Pause, Next, Repeat
- Heart icon for Favorites
- Three-dot menu for song options (Add to Playlist, Details, etc.)

#### 3.1.2 Pro Mode (Toggled On)
**Purpose:** Advanced controls for musicians and learners.

**Trigger:** Tap "Pro Mode" toggle button in top-right corner

**Layout:**
```
┌─────────────────────────────┐
│ [Exit Pro] Song Title       │
├─────────────────────────────┤
│  [Album Art - Small]        │ ← Shrinks to 30% height
├─────────────────────────────┤
│  ┌─────────────────────┐    │
│  │   WAVEFORM VIEW     │    │ ← Visual waveform replaces seek bar
│  │  ▁▃▅▇▅▃▁●▂▄▆▄▂     │    │
│  │  [A────────B]       │    │ ← Highlighted loop segment
│  │  0:45.20   1:23.80  │    │
│  └─────────────────────┘    │
├─────────────────────────────┤
│  [A] [< >] [Loop] [< >] [B] │ ← A-B Loop Controls
├─────────────────────────────┤
│  Speed: [━━●━━━] 0.75x      │ ← Speed Slider
│  [0.5x] [0.75x] [1.0x]      │ ← Quick Presets
├─────────────────────────────┤
│   [⏮] [⏯] [⏭]              │ ← Simplified playback
└─────────────────────────────┘
```

**Elements:**
- Compact album art (thumbnail size)
- **Waveform View:** Replaces standard seek bar, shows audio peaks and valleys
- **A-B Controls:** Set loop points with fine-tune buttons
- **Speed Slider:** Large, easy-to-drag slider with percentage display
- **Quick Preset Buttons:** Tap for instant speed changes
- Simplified playback controls (no shuffle/repeat to reduce clutter)

**Interaction:**
- Toggle between modes preserves playback state
- Pro Mode settings persist per session
- Exit Pro Mode returns to Normal Mode smoothly

### 3.2 Bottom Navigation
Persistent navigation bar with 4 tabs:
1. **Library** (🎵): Songs, Artists, Albums, Genres
2. **Folders** (📁): File system view
3. **Playlists** (📋): User-created and smart playlists
4. **Settings** (⚙️): App preferences and audio settings

### 3.3 Color Scheme & Accessibility
- **Primary Color:** Customizable accent (default: Blue #2196F3)
- **Text Contrast:** WCAG AA compliant (4.5:1 ratio minimum)
- **Touch Targets:** Minimum 48dp x 48dp for all interactive elements
- **Animations:** Smooth transitions (200-300ms), disable option for accessibility

---

## 4. Technical Architecture

### 4.1 Technology Stack
- **Language:** Kotlin (primary), Java (legacy support)
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 (API 34)
- **Architecture:** MVVM (Model-View-ViewModel) with Repository pattern
- **DI Framework:** Hilt or Koin for dependency injection
- **Database:** Room (SQLite abstraction)
- **Media Framework:** ExoPlayer 2.x
- **Audio Effects:** Android AudioEffect API + Sonic/SoundTouch library for speed control

### 4.2 Key Libraries
- **ExoPlayer:** Advanced media playback with format support
- **Sonic Library:** Pitch-preserved speed adjustment
- **Glide/Coil:** Image loading and caching for album art
- **Material Components:** Material Design 3 UI components
- **Coroutines/Flow:** Asynchronous programming and reactive streams
- **WorkManager:** Background tasks (library scanning)

### 4.3 Performance Requirements
- **Cold Start Time:** < 2 seconds on mid-range devices
- **Library Scan:** 1000 songs indexed in < 10 seconds
- **Memory Usage:** < 150MB RAM during active playback
- **Battery Impact:** < 5% drain per hour of playback with screen off
- **APK Size:** < 25MB (initial download)

### 4.4 Data Storage
- **User Preferences:** SharedPreferences (theme, defaults)
- **Music Metadata:** Room database (songs, albums, artists)
- **Playlists:** SQLite tables with song relationships
- **Loop Points:** Per-song storage in database (optional feature)
- **Cache:** Album art and waveforms in internal storage (auto-cleanup if > 500MB)

### 4.5 Permissions
- `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 12-)
- `FOREGROUND_SERVICE` for background playback
- `WAKE_LOCK` for screen-off playback
- `BLUETOOTH_CONNECT` (optional, for headset detection on Android 12+)

---

## 5. User Flows

### 5.1 First Launch Flow
1. User opens app → Permission request screen
2. Grants storage permission → Library scanning starts (progress bar)
3. Scanning completes → Main Library screen shows all songs
4. Onboarding tooltip: "Tap any song to play. Try Pro Mode for advanced controls!"

### 5.2 Setting Up A-B Loop (Musician Use Case)
1. User plays a guitar solo they want to learn
2. Taps "Pro Mode" toggle → Layout shifts to Pro Mode
3. Listens to solo, taps "A" at start of difficult section → Marker appears on waveform
4. Taps "B" at end of section → Segment highlights in blue
5. Taps "Loop" toggle → Solo section repeats continuously
6. Uses < > buttons to fine-tune exact start/end points
7. Adjusts speed slider to 0.5x → Slowed solo loops at natural pitch
8. Practices until comfortable, then increases speed to 0.75x, then 1.0x
9. Exits Pro Mode → Returns to normal listening

### 5.3 Creating a Playlist
1. User navigates to "Playlists" tab
2. Taps "+" FAB (Floating Action Button) → "New Playlist" dialog
3. Enters name "Workout Mix" → Confirms
4. Long-presses a song in Library → "Add to Playlist" option
5. Selects "Workout Mix" → Song added, toast confirmation
6. Repeats for 10-15 songs
7. Opens "Workout Mix" → Reorders songs by dragging
8. Taps play → Playlist begins

---

## 6. Open Questions & Future Enhancements

### 6.1 Open Questions for Stakeholders
1. **Monetization Strategy:**
   - Free with ads (interstitial between songs?)
   - Free with in-app purchase to remove ads ($2.99 one-time?)
   - Subscription for cloud features (future)?
   
2. **Cloud Sync (Future):**
   - Should playlists sync across devices via Google Drive or proprietary backend?
   
3. **Lyrics Source:**
   - Partner with a lyrics API (Musixmatch, Genius) or rely only on local files?

### 6.2 Future Feature Roadmap (Post-MVP)
- **Pitch Shifting:** Change song key without affecting speed (for singers)
- **Voice Isolation:** AI-powered vocal/instrumental separation
- **Gesture Controls:** Swipe to skip, volume control via vertical swipe
- **Android Auto / Android TV:** Full support for car and TV platforms
- **Chromecast:** Stream music to smart speakers
- **Smart Playlists:** Auto-generate playlists based on BPM, mood, or listening history
- **Social Features:** Share playlists with friends, collaborative playlists

---

## 7. Success Criteria & KPIs

### 7.1 Launch Criteria (MVP)
✅ All Priority 1 features implemented and tested  
✅ Crash rate < 2% on top 10 Android devices  
✅ App size < 25MB  
✅ 4+ star internal QA rating  
✅ Accessibility: TalkBack compatible, touch targets meet guidelines  

### 7.2 Post-Launch KPIs (3 Months)
- **Downloads:** 50,000+ installs
- **Retention:** 30-day retention rate > 35%
- **Ratings:** Average rating > 4.3 stars with 500+ reviews
- **Engagement:** Average session duration > 20 minutes
- **Pro Mode Adoption:** 25% of active users toggle Pro Mode at least once

### 7.3 User Feedback Channels
- In-app feedback form (Settings → "Send Feedback")
- Google Play Store reviews monitoring (weekly)
- Beta testing program via Google Play (100 testers)
- Reddit community for power users (r/YourAppName)

---

## 8. Development Timeline (Estimate)

### Phase 1: Foundation (4 weeks)
- Project setup, architecture design
- Library scanning and categorization
- Basic playback (play/pause/next/prev)
- Folder view implementation

### Phase 2: Core Features (6 weeks)
- A-B Loop with fine-tuning
- Speed control with pitch preservation
- Playlist management
- Equalizer and audio effects

### Phase 3: UI/UX Polish (4 weeks)
- Pro Mode implementation
- Waveform view
- Themes (Light/Dark/AMOLED)
- Lyrics display
- Album art editor

### Phase 4: System Integration (3 weeks)
- Notification controls
- Lock screen media session
- Headset button handling
- Background playback optimization

### Phase 5: Testing & Refinement (3 weeks)
- Internal QA testing
- Beta release to 100 users
- Bug fixes and performance optimization
- Accessibility audit

### Phase 6: Launch (1 week)
- Final QA sign-off
- Google Play Store listing preparation
- Marketing materials (screenshots, video)
- Public release

**Total Estimated Timeline:** 21 weeks (~5 months)

---

## 9. Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Pitch preservation quality issues | High | Medium | Early testing with Sonic library, fallback to simpler algorithm if needed |
| Performance on low-end devices | Medium | High | Extensive testing on budget devices, optimize waveform rendering |
| MediaStore permission issues (Android 11+) | High | Medium | Implement scoped storage correctly, provide clear permission explanations |
| Waveform generation crashes on large files | Medium | Medium | Implement background processing with timeouts, show placeholder for huge files |
| Battery drain complaints | Medium | High | Optimize foreground service, add battery saver mode in settings |

---

## 10. Appendix

### 10.1 Competitor Analysis
- **Poweramp:** Strong EQ, lacks intuitive A-B loop
- **Musicolet:** Great folder view, no speed control
- **Neutron:** Professional audio, complex UI
- **Our Advantage:** Best-in-class A-B loop + speed control with musician-friendly UX

### 10.2 Glossary
- **A-B Loop:** Repeating a specific segment of audio between two points
- **Pitch Preservation:** Maintaining natural audio frequency when changing playback speed
- **Gapless Playback:** Seamless transitions between tracks with no silence
- **AMOLED Black:** Pure black theme optimized for OLED screens to save battery

### 10.3 References
- [Android Media Playback Guide](https://developer.android.com/guide/topics/media/mediaplayer)
- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Material Design 3 Guidelines](https://m3.material.io/)
- [Sonic Library (Speed/Pitch)](https://github.com/waywardgeek/sonic)

---

**Document Version:** 1.0  
**Last Updated:** January 24, 2026  
**Author:** Product Team  
**Approvers:** Engineering Lead, Design Lead, Product Manager