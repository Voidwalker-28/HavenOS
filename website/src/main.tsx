import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'

type Theme = 'light' | 'dark'
type PageKey = 'landing' | 'product' | 'privacy' | 'faq'

const DOWNLOAD_URL = 'https://github.com/Voidwalker-28/HavenOS/releases/download/v1.0/HavenOS-vc10.apk'

type ScreenshotProps = {
  src: string
  alt: string
  label: string
  note?: string
  variant?: 'hero' | 'wide' | 'compact' | 'dialog'
  tilt?: string
}

const permissionRows = [
  ['Accessibility', 'Enables edge gestures.', 'Optional'],
  ['Device admin', 'Enables the lock-screen action.', 'Optional'],
  ['Notification access', 'Provides notification badges and Now Playing.', 'Optional'],
  ['Calendar', 'Shows upcoming calendar events in Today.', 'Optional'],
  ['Contacts', 'Lets search include contacts.', 'Optional'],
  ['Draw over apps', 'Supports the launcher’s overlay surfaces.', 'Optional'],
  ['Internet', 'Fetches weather from Open-Meteo.', 'Required for weather'],
]

const faqItems = [
  {
    question: 'What is HavenOS?',
    answer: 'HavenOS is a customisable, tablet-aware Android launcher. It gives you more control over the home surface, the app drawer, search, gestures, look, and layout recovery.',
  },
  {
    question: 'Can I keep the home screen calm?',
    answer: 'Yes. Calm mode lets you choose up to eight favorites and schedule when that smaller set is shown. The Today column can keep date, weather, next calendar events, and now playing in view.',
  },
  {
    question: 'What can I change?',
    answer: 'You can change home pages, grid density, snap-to-grid, folders, stacks, widgets, dock behavior, icon treatment, wallpaper tint and zoom, corner radius, custom hex color, and motion reduction.',
  },
  {
    question: 'What happens if I make a mess?',
    answer: 'HavenOS includes layout snapshots, undo and revert for a session, restore layout, multiple layout profiles with schedules, and backup export/import.',
  },
  {
    question: 'Why does HavenOS ask for permissions?',
    answer: 'Each permission maps to a visible feature: accessibility for edge gestures, device admin for the lock-screen action, notification access for badges and Now Playing, calendar and contacts for search and Today, draw over apps for overlays, and internet for weather. See the full table on the Privacy page.',
  },
  {
    question: 'Where does weather come from?',
    answer: 'Weather is sourced from Open-Meteo. HavenOS needs internet access for that weather surface.',
  },
  {
    question: 'What is still unknown?',
    answer: 'The verified repository release is v1.0 (versionCode 10), licensed MIT, with Android 8.0 (API 26) as the minimum. The remaining product details not supplied are the tagline, price, developer name, and privacy policy effective date.',
  },
]

function usePage(): PageKey {
  const path = window.location.pathname.replace(/\/+$/, '') || '/'
  if (path === '/product') return 'product'
  if (path === '/privacy') return 'privacy'
  if (path === '/faq') return 'faq'
  return 'landing'
}

function App() {
  const page = usePage()
  const [theme, setTheme] = useState<Theme>(() => {
    const stored = window.localStorage.getItem('havenos-theme') as Theme | null
    return stored || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  })
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('havenos-theme', theme)
  }, [theme])

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
    const titles: Record<PageKey, string> = {
      landing: 'HavenOS — A home screen with a way back',
      product: 'HavenOS — The calm launcher',
      privacy: 'HavenOS Privacy — Permissions in plain words',
      faq: 'HavenOS FAQ — Answers before you install',
    }
    document.title = titles[page]
  }, [page])

  const handleDownload = () => {
    window.location.assign(DOWNLOAD_URL)
  }

  return (
    <div className={`site-shell page-${page}`}>
      {page !== 'landing' && <Header theme={theme} onToggle={() => setTheme(theme === 'light' ? 'dark' : 'light')} onDownload={handleDownload} />}
      {page === 'landing' && <LandingPage onDownload={handleDownload} />}
      {page === 'product' && <HomePage onDownload={handleDownload} />}
      {page === 'privacy' && <PrivacyPage />}
      {page === 'faq' && <FaqPage />}
      <Footer onDownload={handleDownload} />
    </div>
  )
}

function LandingPage({ onDownload }: { onDownload: () => void }) {
  const [introDone, setIntroDone] = useState(false)
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { setIntroDone(true); return }
    const timer = window.setTimeout(() => setIntroDone(true), 2050)
    return () => window.clearTimeout(timer)
  }, [])
  return (
    <main className={`landing-page ${introDone ? 'intro-done' : ''}`}>
      <div className="landing-intro" aria-hidden="true"><span>make room for what matters</span></div>
      <nav className="landing-nav" aria-label="Landing navigation">
        <a className="landing-wordmark" href="/"><img src="/havenos-logo.svg" alt="" /><span>haven<span>os</span></span></a>
        <div className="landing-nav-links"><a href="#engine">What it does</a><a href="#spectrum">How it feels</a><a href="/faq">FAQ</a></div>
        <a className="landing-nav-cta" href="/product">Enter HavenOS <span>↗</span></a>
      </nav>
      <section className="landing-hero">
        <RibbonCanvas />
        <div className="landing-veil" />
        <div className="landing-hero-content">
          <div className="landing-meta"><span>01 · HavenOS</span><span>Android launcher / app.havenos.launcher</span><span>v1.0 / vc10</span></div>
          <h1>More room<br /><em>to be there.</em></h1>
          <p>A calmer place to start, with the controls to shape every page behind it. HavenOS is a tablet-aware Android launcher for people who want less noise without giving up the keys.</p>
          <div className="landing-actions"><a className="landing-button landing-button-primary" href="/product">Explore the launcher <span>↗</span></a><button className="landing-button landing-button-quiet" type="button" onClick={onDownload}>Download APK <span>↓</span></button></div>
        </div>
      </section>

      <LandingBand id="engine" number="01 · Surfaces" title="A smaller default.<br />A deeper control room." lede="Start with a quiet home. Keep the useful complexity close: Today, Calm mode, folders, stacks, widgets, dock control, and one search that reaches further than your app drawer.">
        <div className="landing-engine-grid">
          <EngineTile index="01" title="Today" text="Date, weather, next events, and now playing in one considered rail." kind="wave" />
          <EngineTile index="02" title="Calm mode" text="Choose up to eight favorites and schedule when the rest steps aside." kind="blocks" />
          <EngineTile index="03" title="Home + dock" text="Pages, density, snap-to-grid, folders, stacks, widgets, and a dock that stays yours." kind="lines" />
          <EngineTile index="04" title="Safety net" text="Snapshots, undo, revert session, restore layout, profiles, and backup import/export." kind="ring" />
        </div>
      </LandingBand>

      <LandingBand id="spectrum" number="02 · Search" title="One place to look.<br /><em>Nothing else to learn.</em>" lede="Search apps, settings, contacts, calendar events, and the web from one surface. Typed sums work there too. The mechanism is visible because the launcher should feel legible, not mysterious.">
        <SpectrumCanvas />
      </LandingBand>

      <LandingBand id="proof" number="03 · Product facts" title="Quiet by default.<br />Specific by design." lede="The product is small enough to understand and deep enough to grow with you. These are the parts that are verified in the repository and the supplied recording.">
        <div className="landing-stats"><div><strong><i>8</i></strong><span>chosen Calm favorites</span></div><div><strong><i>1</i></strong><span>search surface for apps, settings, people, events, web</span></div><div><strong><i>26</i></strong><span>minimum Android API level</span></div><div><strong><i>0</i></strong><span>accounts, ads, or analytics in the repository description</span></div></div>
      </LandingBand>

      <section className="landing-faq landing-band" id="questions"><div className="landing-band-head"><div className="landing-band-label"><span>04 · Questions</span><i /></div><div className="landing-band-title"><h2>Read before<br /><em>you change home.</em></h2><p>Short answers about permissions, calmer modes, the app drawer, and what happens when you change your mind.</p></div></div><div className="landing-faq-list">{faqItems.slice(0, 5).map((item, index) => <FaqItem key={item.question} item={item} index={index} />)}</div></section>

      <section className="landing-close"><p>Keep the home<br /><em>you built.</em></p><div className="landing-actions"><a className="landing-button landing-button-primary" href="/product">See the product page <span>↗</span></a><button className="landing-button landing-button-quiet" type="button" onClick={onDownload}>Download APK <span>↓</span></button></div></section>
      <footer className="landing-footer"><span>HavenOS / a calm, configurable home</span><span>MIT · Android 8.0+ · v1.0 / vc10</span><span><a href="/privacy">Privacy</a> · <a href="/faq">FAQ</a></span></footer>
    </main>
  )
}

function LandingBand({ id, number, title, lede, children }: { id: string; number: string; title: string; lede: string; children: React.ReactNode }) {
  return <section className="landing-band" id={id}><div className="landing-band-head"><div className="landing-band-label"><span>{number}</span><i /></div><div className="landing-band-title"><h2 dangerouslySetInnerHTML={{ __html: title }} /><p>{lede}</p></div></div>{children}</section>
}

function RibbonCanvas() {
  const ref = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const canvas = ref.current; if (!canvas) return
    const ctx = canvas.getContext('2d'); if (!ctx) return
    let frame = 0; let raf = 0
    const draw = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2); const w = canvas.clientWidth; const h = canvas.clientHeight
      if (canvas.width !== w * dpr || canvas.height !== h * dpr) { canvas.width = w * dpr; canvas.height = h * dpr }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, w, h)
      const t = frame * .008
      ;[['#6366f1', 0, .18], ['#8b5cf6', 1.7, .25], ['#22d3ee', 3.2, .32]].forEach(([color, phase, width]) => {
        ctx.beginPath(); ctx.lineWidth = Math.max(12, h * .04); ctx.lineCap = 'round'; ctx.strokeStyle = color as string; ctx.globalAlpha = .55
        for (let x = -20; x <= w + 20; x += 8) { const y = h * (Number(width) + .12 * Math.sin(x / Math.max(150, w * .2) + t + Number(phase)) + .05 * Math.sin(x / 65 - t * 1.6 + Number(phase))); x === -20 ? ctx.moveTo(x, y) : ctx.lineTo(x, y) }
        ctx.stroke()
      })
      ctx.globalAlpha = 1; frame += 1; raf = requestAnimationFrame(draw)
    }
    draw(); return () => cancelAnimationFrame(raf)
  }, [])
  return <canvas className="ribbon-canvas" ref={ref} aria-hidden="true" />
}

function EngineTile({ index, title, text, kind }: { index: string; title: string; text: string; kind: string }) {
  return <article className="engine-tile"><div className={`engine-canvas engine-${kind}`}><i /><i /><i /><i /></div><span className="engine-index">{index}</span><h3>{title}</h3><p>{text}</p></article>
}

function SpectrumCanvas() {
  const ref = useRef<HTMLCanvasElement>(null)
  useEffect(() => { const canvas = ref.current; if (!canvas) return; const ctx = canvas.getContext('2d'); if (!ctx) return; let raf = 0; let frame = 0; const draw = () => { const dpr = Math.min(devicePixelRatio || 1, 2); const w = canvas.clientWidth; const h = canvas.clientHeight; canvas.width = w * dpr; canvas.height = h * dpr; ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, w, h); const gap = w / 72; for (let i = 0; i < 72; i += 1) { const env = Math.sin((i / 71) * Math.PI) * .72 + .18; const wave = .62 + .3 * Math.sin(frame * .03 + i * .45); const selected = i >= 12 && i <= 31; const bar = env * wave * h * .72 + (selected ? h * .08 : 0); ctx.fillStyle = selected ? '#6366f1' : 'rgba(255,255,255,.16)'; ctx.fillRect(i * gap + 1, h - bar - 24, Math.max(2, gap - 3), bar) } ctx.strokeStyle = 'rgba(99,102,241,.8)'; ctx.strokeRect(12 * gap, h * .12, 20 * gap, h * .72); frame += 1; raf = requestAnimationFrame(draw) }; draw(); return () => cancelAnimationFrame(raf) }, [])
  return <div className="spectrum-wrap"><canvas ref={ref} aria-label="Animated spectrum with one selected search band" /><span className="spectrum-caption">Selected surface / search reaches across the launcher</span><strong className="spectrum-readout">42 × 6 = 252</strong></div>
}

function Header({ theme, onToggle, onDownload }: { theme: Theme; onToggle: () => void; onDownload: () => void }) {
  return (
    <header className="site-header">
      <a className="wordmark" href="/product" aria-label="HavenOS product home">
        <img className="wordmark-logo" src="/havenos-logo.svg" alt="" aria-hidden="true" />
        <span>haven<span className="wordmark-accent">os</span></span>
      </a>
      <nav className="main-nav" aria-label="Primary navigation">
        <a href="/product#calm-control">Calm / control</a>
        <a href="/product#safety-net">Safety net</a>
        <a href="/privacy">Permissions</a>
        <a href="/faq">FAQ</a>
      </nav>
      <div className="header-actions">
        <button className="theme-toggle" type="button" onClick={onToggle} aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}>
          <span className="theme-dot" aria-hidden="true" />
          {theme === 'light' ? 'Light' : 'Dark'}
        </button>
        <button className="text-button compact" type="button" onClick={onDownload}>Download APK <span aria-hidden="true">↗</span></button>
      </div>
    </header>
  )
}

function HomePage({ onDownload }: { onDownload: () => void }) {
  return (
    <main>
      <section className="hero immersive-hero section-shell" id="home">
        <video className="hero-video" src="/assets/havenos-hero.mp4" muted autoPlay loop playsInline preload="auto" onLoadedMetadata={(event) => { event.currentTarget.playbackRate = 0.65 }} aria-label="Looping screen recording of HavenOS home and settings surfaces" />
        <div className="hero-video-overlay" aria-hidden="true" />
        <div className="hero-grain" aria-hidden="true" />
        <div className="hero-watermark" aria-hidden="true">HAVENOS</div>
        <div className="immersive-hero-content">
          <div className="hero-copy immersive-copy reveal-up">
            <p className="eyebrow"><span className="eyebrow-line" /> Android launcher / app.havenos.launcher</p>
            <h1>A home screen with<br /><em>a way back.</em></h1>
            <p className="hero-lede">HavenOS gives you a quieter place to start, with the control to shape every page behind it.</p>
            <div className="hero-actions">
              <button className="primary-button" type="button" onClick={onDownload}>Download APK <span aria-hidden="true">↗</span></button>
              <a className="quiet-link" href="#calm-control">See how it works <span aria-hidden="true">↓</span></a>
            </div>
            <div className="hero-facts" aria-label="Product facts">
              <div><span className="fact-label">Built for</span><strong>Android</strong></div>
              <div><span className="fact-label">Knows about</span><strong>Tablets</strong></div>
              <div><span className="fact-label">Latest release</span><strong>v1.0 / vc10</strong></div>
            </div>
          </div>
          <div className="hero-stack" aria-label="HavenOS product surfaces">
            <div className="glass-stat-card"><span>01 / Today</span><strong>See the day<br />without the feed.</strong><small>Date, weather, events, now playing.</small></div>
            <div className="glass-stat-card"><span>02 / Safety net</span><strong>Try a new layout.<br />Keep a way back.</strong><small>Snapshots, revert session, restore layout.</small></div>
            <div className="glass-stat-card"><span>03 / Recorded UI</span><strong>Real screens.<br />No invented mockups.</strong><small>12-second loop from the supplied recording.</small></div>
          </div>
        </div>
        <div className="hero-bottom-rail" aria-label="HavenOS feature links"><a href="#today-calm">Today & Calm</a><a href="#safety-net">Undo anything</a><a href="#permissions">Permissions, plainly</a><span>Scroll to explore ↓</span></div>
      </section>

      <section className="intro-band section-shell" id="calm-control">
        <div className="section-kicker">The premise</div>
        <div className="intro-copy">
          <h2>Calm when you want it.<br /><span>Control when you need it.</span></h2>
          <p>HavenOS is not a stripped-back launcher pretending you never change your mind. It gives you a smaller default, then leaves the deeper controls close by.</p>
        </div>
        <CalmControl />
      </section>

      <section className="feature-section section-shell" id="today-calm">
        <div className="feature-index">02 / Today & Calm</div>
        <div className="feature-copy">
          <p className="eyebrow">A starting point, not a feed</p>
          <h2>See what matters<br /><em>today.</em></h2>
          <p>The Today column puts date, weather, next calendar events, and now playing in one place. When you want less, Calm mode shows up to eight chosen favorites and can follow a schedule.</p>
          <a className="quiet-link" href="/faq">Read the FAQ <span aria-hidden="true">↗</span></a>
        </div>
        <div className="today-card-stack" aria-label="Today and Calm feature notes">
          <div className="note-card accent-card"><span className="card-index">A</span><strong>Today column</strong><span>Date / weather / events / now playing</span></div>
          <div className="note-card"><span className="card-index">B</span><strong>Calm mode</strong><span>Up to 8 favorites, schedulable</span></div>
          <div className="note-card dark-card"><span className="card-index">C</span><strong>Open-Meteo</strong><span>Weather source, stated plainly</span></div>
        </div>
      </section>

      <section className="feature-section split-section section-shell" id="drawer-search">
        <div className="split-visual">
          <ScreenshotPlate
            src="/assets/edit-home.webp"
            alt="HavenOS edit home screen showing undo, redo, snap to grid, add, pages, wallpaper, look, calm, settings, and done controls."
            label="Observed / edit home"
            note="Edit mode controls visible in the recording"
            variant="wide"
            tilt="2deg"
          />
        </div>
        <div className="feature-copy numbered-copy">
          <div className="feature-index">03 / Home & dock</div>
          <h2>Put the pieces<br /><em>where they belong.</em></h2>
          <p>Build multiple pages with your preferred grid density. Snap things into place, use folders, stacks, and widgets, and shape the dock with slots, labels, position, or a summon-only state. Lock the layout when it feels right.</p>
          <div className="inline-list"><span>Grid density</span><span>Folders + stacks</span><span>Widgets</span><span>Lock layout</span></div>
        </div>
      </section>

      <section className="feature-section bento-section section-shell" id="gestures">
        <div className="bento-heading">
          <div className="feature-index">04 / Drawer, search, gestures</div>
          <h2>One drawer.<br /><em>Many ways in.</em></h2>
        </div>
        <div className="bento-grid">
          <div className="bento-card bento-large">
            <span className="bento-mark">01</span>
            <h3>Search once</h3>
            <p>Search apps, settings, contacts, calendar events, and the web from one place. Typed sums work there too.</p>
            <div className="search-slab"><span>⌕</span><span>42 × 6</span><b>= 252</b></div>
          </div>
          <div className="bento-card bento-dark">
            <span className="bento-mark">02</span>
            <h3>Sort your library</h3>
            <p>Use a library view and sort by newest install date when you need to find the last thing you added.</p>
            <div className="sort-lines"><span>Newest installed <b>↕</b></span><span>Alphabetical <b>·</b></span><span>Hidden apps <b>locked</b></span></div>
          </div>
          <div className="bento-card bento-accent">
            <span className="bento-mark">03</span>
            <h3>Make edges useful</h3>
            <p>Icon swipes, edge swipes, double tap, pinch, two-finger down, and swipe-up drawer. Tune sensitivity and haptics.</p>
            <div className="gesture-orbit" aria-hidden="true"><span>↖</span><i /><span>↘</span></div>
          </div>
        </div>
      </section>

      <section className="feature-section look-section section-shell" id="style">
        <div className="feature-copy">
          <div className="feature-index">05 / Look</div>
          <p className="eyebrow">The surface is part of the system</p>
          <h2>Make it yours,<br /><em>then turn it down.</em></h2>
          <p>Choose blurred-glass or flat looks, themed icons, icon packs, icon shape, corner radius, wallpaper tint/dim/zoom, custom hex color, and reduced effects or motion.</p>
          <div className="swatch-row" aria-label="HavenOS palette tokens"><i /><i /><i /><i /><i /></div>
        </div>
        <div className="look-visual">
          <ScreenshotPlate
            src="/assets/look-settings.webp"
            alt="HavenOS Look settings showing Minimal, Dark, Frosted, accent palette, appearance mode, and material controls."
            label="Observed / Look settings"
            note="Visible settings labels are grounded in the recording"
            variant="wide"
            tilt="-2deg"
          />
        </div>
      </section>

      <section className="safety-section section-shell" id="safety-net">
        <div className="safety-copy">
          <div className="feature-index">06 / The safety net</div>
          <p className="eyebrow">No ceremony required</p>
          <h2>Undo anything<br /><em>without starting over.</em></h2>
          <p>Try a new arrangement, then step back. HavenOS keeps layout snapshots, an undo/revert session path, restore layout, multiple profiles with schedules, and backup export/import within the same story.</p>
          <div className="safety-actions"><span>Layout snapshot</span><span>Revert session</span><span>Profile schedule</span><span>Backup export / import</span></div>
        </div>
        <ScreenshotPlate
          src="/assets/changes-dialog.webp"
          alt="HavenOS Changes dialog over the home screen with Revert, Revert session, and Done actions."
          label="Observed / changes dialog"
          note="A real recovery moment from the supplied recording"
          variant="dialog"
          tilt="3deg"
        />
      </section>

      <PermissionsSection />

      <section className="faq-preview section-shell" id="faq">
        <div className="faq-preview-top">
          <div><div className="feature-index">08 / Before you install</div><h2>Good questions<br /><em>are part of the product.</em></h2></div>
          <a className="quiet-link" href="/faq">Open all answers <span aria-hidden="true">↗</span></a>
        </div>
        <div className="faq-preview-grid">
          {faqItems.slice(0, 3).map((item, index) => <FaqItem key={item.question} item={item} index={index} />)}
        </div>
      </section>
    </main>
  )
}

function CalmControl() {
  const [mode, setMode] = useState<'calm' | 'control'>('calm')
  const calm = mode === 'calm'
  return (
    <div className="calm-control">
      <div className="mode-tabs" role="tablist" aria-label="Calm and control modes">
        <button type="button" role="tab" aria-selected={calm} className={calm ? 'is-active' : ''} onClick={() => setMode('calm')}>Calm</button>
        <button type="button" role="tab" aria-selected={!calm} className={!calm ? 'is-active' : ''} onClick={() => setMode('control')}>Full control</button>
      </div>
      <div className="mode-panel">
        <div className="mode-copy">
          <span className="mode-number">{calm ? '01' : '02'}</span>
          <h3>{calm ? 'A smaller default.' : 'The controls are still there.'}</h3>
          <p>{calm ? 'Schedule up to eight chosen favorites for the moments you want fewer decisions.' : 'Open the drawer, edit the home, tune gestures, or change the look when you want more room to shape things.'}</p>
        </div>
        <div className="mode-visual">
          <img src={calm ? '/assets/home-screen.webp' : '/assets/edit-home.webp'} alt={calm ? 'Observed HavenOS home screen with Today widgets and app clusters.' : 'Observed HavenOS edit home controls over the home screen.'} />
          <span>{calm ? 'today / calm' : 'edit / tune'}</span>
        </div>
      </div>
    </div>
  )
}

function PermissionsSection() {
  return (
    <section className="permissions-section section-shell" id="permissions">
      <div className="permissions-heading">
        <div className="feature-index">07 / Permissions</div>
        <h2>Nothing hidden<br /><em>in the fine print.</em></h2>
        <p>HavenOS asks for access when a feature needs it. Here is what each permission does, in plain words.</p>
        <a className="quiet-link" href="/privacy">Read the privacy page <span aria-hidden="true">↗</span></a>
      </div>
      <PermissionTable />
      <p className="permission-footnote"><span className="footnote-mark">[Optional]</span> An ADB grant can optionally hide the navigation bar. It is not described as required.</p>
    </section>
  )
}

function PermissionTable() {
  return (
    <div className="permission-table-wrap">
      <table className="permission-table">
        <thead><tr><th>Permission</th><th>Why HavenOS uses it</th><th>Optional?</th></tr></thead>
        <tbody>{permissionRows.map(([permission, why, optional]) => <tr key={permission}><th scope="row">{permission}</th><td>{why}</td><td><span className={optional === 'Required for weather' ? 'required-pill' : 'optional-pill'}>{optional}</span></td></tr>)}</tbody>
      </table>
    </div>
  )
}

function PrivacyPage() {
  return (
    <main className="inner-page">
      <section className="page-hero section-shell">
        <div className="feature-index">HavenOS / Privacy</div>
        <h1>Permissions,<br /><em>in plain words.</em></h1>
        <p>Access should map to a visible action. This page names the Android permissions in the HavenOS brief and what each one enables.</p>
      </section>
      <section className="privacy-content section-shell">
        <div className="privacy-intro"><span className="eyebrow-line" /><p>Package name: <code>app.havenos.launcher</code></p></div>
        <PermissionTable />
        <div className="privacy-notes">
          <div><span className="note-label">Optional ADB grant</span><h2>Hiding the navigation bar is an optional extra.</h2><p>The brief identifies an optional ADB grant for hiding the navigation bar. It is not presented as a prerequisite for using the launcher.</p></div>
          <div><span className="note-label">Verified from the repository</span><h2>MIT · Android 8.0+ · v1.0.</h2><p>The repository identifies the license as MIT, the minimum Android version as 8.0 (API 26), and the latest release as v1.0 (versionCode 10). Developer name and policy effective date remain `[TODO]`.</p></div>
        </div>
      </section>
    </main>
  )
}

function FaqPage() {
  return (
    <main className="inner-page">
      <section className="page-hero faq-hero section-shell">
        <div className="feature-index">HavenOS / FAQ</div>
        <h1>Answers before<br /><em>you install.</em></h1>
        <p>Short, specific answers about the launcher, its calmer modes, its control surfaces, and the permissions behind them.</p>
      </section>
      <section className="faq-page-content section-shell">
        <div className="faq-page-intro"><span className="eyebrow-line" /><p>Start with the question you actually have.</p></div>
        <div className="faq-list">{faqItems.map((item, index) => <FaqItem key={item.question} item={item} index={index} />)}</div>
      </section>
    </main>
  )
}

function FaqItem({ item, index }: { item: { question: string; answer: string }; index: number }) {
  const [open, setOpen] = useState(index === 0)
  return (
    <article className={`faq-item ${open ? 'is-open' : ''}`}>
      <button type="button" className="faq-question" aria-expanded={open} onClick={() => setOpen(!open)}>
        <span className="faq-index">0{index + 1}</span><span>{item.question}</span><span className="faq-toggle" aria-hidden="true">{open ? '−' : '+'}</span>
      </button>
      <div className="faq-answer" hidden={!open}><p>{item.answer}</p></div>
    </article>
  )
}

function ScreenshotPlate({ src, alt, label, note, variant = 'wide', tilt = '0deg' }: ScreenshotProps) {
  const style = { '--tilt': tilt } as CSSProperties
  return (
    <figure className={`screenshot-plate ${variant}`} style={style}>
      <div className="plate-shadow" />
      <div className="plate-surface"><img src={src} alt={alt} loading={variant === 'hero' ? 'eager' : 'lazy'} /></div>
      <figcaption><span>{label}</span><small>{note}</small></figcaption>
    </figure>
  )
}

function Footer({ onDownload }: { onDownload: () => void }) {
  return (
    <footer className="site-footer" id="download">
      <div className="footer-main section-shell">
        <div className="footer-pitch"><a className="wordmark" href="/"><img className="wordmark-logo" src="/havenos-logo.svg" alt="" aria-hidden="true" /><span>haven<span className="wordmark-accent">os</span></span></a><h2>Keep the home<br /><em>you built.</em></h2><button className="primary-button" type="button" onClick={onDownload}>Download APK <span aria-hidden="true">↗</span></button></div>
        <div className="footer-links"><div><span className="footer-label">Explore</span><a href="/product#calm-control">Calm / control</a><a href="/product#safety-net">Safety net</a><a href="/product#permissions">Permissions</a></div><div><span className="footer-label">Read</span><a href="/privacy">Privacy</a><a href="/faq">FAQ</a><a href="/product">Product</a></div><div><span className="footer-label">Product</span><span>Android launcher</span><span>app.havenos.launcher</span><span>Latest release: v1.0 / vc10</span></div></div>
      </div>
      <div className="footer-bottom section-shell"><span>HavenOS / a calm, configurable home</span><span>Tagline · price · developer name: [TODO]</span></div>
    </footer>
  )
}

createRoot(document.getElementById('root')!).render(<App />)

export default App
