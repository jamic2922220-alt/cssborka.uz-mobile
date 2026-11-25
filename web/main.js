/* X CLIENT Web: CS 1.6 brauzerda (iPhone Safari, Android, kompyuter).
 * Dvijok: xash3d-fwgs (WebAssembly, MIT), o'yin: bizning cs16 client (WebAssembly).
 * O'yin fayllari bir marta yuklanadi va brauzer keshida (Cache Storage) saqlanadi.
 * Tarmoq: brauzer UDP yubora olmaydi - paketlar WebSocket orqali ws_relay.py ga, u serverga UDP qilib yuboradi. */
import { Xash3D, Net } from 'xash3d-fwgs';
import { inflateSync } from 'fflate';

const Q = new URLSearchParams(location.search);
/* avval sayt bilan bir xil 443-port (nginx /xcws/ -> ko'prik), ulanmasa - to'g'ridan-to'g'ri 27080 */
const RELAYS = Q.get('relay') ? [Q.get('relay')] : ['wss://mmb.boost-top.com/xcws/', 'wss://mmb.boost-top.com:27080/'];
const DATA_URL = 'xc_web_data.zip';
const DATA_VER = '__DATA_VER__';                 // CI almashtiradi - arxiv yangilansa qayta yuklanadi
const $ = (id) => document.getElementById(id);
const say = (t) => { $('st').textContent = t; };
const bar = (p) => { $('bar').style.width = Math.max(0, Math.min(100, p)) + '%'; };

/* ---------- tarmoq: WebSocket ko'prik ---------- */
class WsSender {
  constructor(urls) { this.urls = urls; this.ui = 0; this.q = []; this.net = null; this.connect(); }
  connect() {
    const url = this.urls[this.ui % this.urls.length];
    let opened = false;
    const ws = new WebSocket(url);
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => { opened = true; for (const b of this.q) ws.send(b); this.q = []; };
    ws.onmessage = (e) => {
      const d = new Uint8Array(e.data);
      if (d.length < 6 || !this.net) return;
      this.net.incoming.push({ ip: [d[0], d[1], d[2], d[3]], port: (d[4] << 8) | d[5], data: d.subarray(6) });
    };
    ws.onclose = () => { if (!opened) this.ui++; setTimeout(() => this.connect(), opened ? 1500 : 300); };   // ochilmadi - keyingi manzil
    this.ws = ws;
  }
  sendto({ data, ip, port }) {
    const b = new Uint8Array(6 + data.length);
    b.set(ip, 0); b[4] = port >> 8; b[5] = port & 255; b.set(data, 6);
    if (this.ws.readyState === 1) this.ws.send(b);
    else if (this.q.length < 256) this.q.push(b);
  }
}
/* "connect 212.47.58.89:27015" - oddiy IP manzillarni o'zgartirmasdan beramiz (asl Net faqat ichki nomlarni biladi) */
class XcNet extends Net {
  getaddrinfo(hostPtr, r, h, aiPtr) {
    const host = this.em.AsciiToString(hostPtr);
    if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(host)) return super.getaddrinfo(hostPtr, r, h, aiPtr);
    const sa = this.em._malloc(16);
    this.em.writeSockaddr(sa, 2, host, 0);
    const ai = this.em._malloc(32);
    this.em.HEAP32[(ai + 4) >> 2] = 2; this.em.HEAP32[(ai + 8) >> 2] = 2; this.em.HEAP32[(ai + 12) >> 2] = 17;
    this.em.HEAPU32[(ai + 24) >> 2] = 0; this.em.HEAPU32[(ai + 20) >> 2] = sa;
    this.em.HEAP32[(ai + 16) >> 2] = 16; this.em.HEAP32[(ai + 28) >> 2] = 0;
    this.em.HEAPU32[aiPtr >> 2] = ai;
    return 0;
  }
}

/* ---------- o'yin fayllari: yuklash (progress bilan) va keshlash ---------- */
async function getData() {
  let cache = null;
  try { cache = await caches.open('xc-data'); } catch (e) {}
  const key = DATA_URL + '?v=' + DATA_VER;
  if (cache) {
    const hit = await cache.match(key);
    if (hit) { say("O'yin fayllari telefondan o'qilmoqda..."); bar(100); return new Uint8Array(await hit.arrayBuffer()); }
    for (const k of await cache.keys()) await cache.delete(k);      // eski versiya
  }
  const res = await fetch(key);
  if (!res.ok) throw new Error('fayl ' + res.status);
  const total = +res.headers.get('content-length') || 0;
  const rd = res.body.getReader(); const parts = []; let got = 0;
  for (;;) {
    const { done, value } = await rd.read();
    if (done) break;
    parts.push(value); got += value.length;
    if (total) { bar(got * 100 / total); say("O'yin fayllari yuklanmoqda: " + Math.floor(got * 100 / total) + '% (' + (got >> 20) + ' / ' + (total >> 20) + ' MB)'); }
  }
  const buf = new Uint8Array(got); let o = 0;
  for (const p of parts) { buf.set(p, o); o += p.length; }
  parts.length = 0;
  if (cache) { try { await cache.put(key, new Response(buf)); } catch (e) { /* joy yo'q - keyingi safar qayta yuklanadi */ } }
  return buf;
}
/* zip ni fayllab ochib, to'g'ridan-to'g'ri dvijok FS ga yozamiz (hammasini birdaniga ochmaymiz - xotira tejaladi) */
async function mountZip(FS, z, root) {
  const u16 = (i) => z[i] | (z[i + 1] << 8), u32 = (i) => (z[i] | (z[i + 1] << 8) | (z[i + 2] << 16) | (z[i + 3] << 24)) >>> 0;
  let e = z.length - 22; while (e >= 0 && u32(e) !== 0x06054b50) e--;
  if (e < 0) throw new Error('arxiv buzilgan');
  const n = u16(e + 10); let p = u32(e + 16);
  const dec = new TextDecoder();
  for (let k = 0; k < n; k++) {
    const meth = u16(p + 10), csz = u32(p + 20), nl = u16(p + 28), el = u16(p + 30), cl = u16(p + 32), lho = u32(p + 42);
    const name = dec.decode(z.subarray(p + 46, p + 46 + nl));
    p += 46 + nl + el + cl;
    if (name.endsWith('/') || name.includes('..')) continue;
    const ds = lho + 30 + u16(lho + 26) + u16(lho + 28);
    const raw = z.subarray(ds, ds + csz);
    const data = meth === 8 ? inflateSync(raw) : raw;
    const path = root + '/' + name;
    FS.mkdirTree(path.slice(0, path.lastIndexOf('/')));
    FS.writeFile(path, data);
    if (k % 200 === 0) { say('Fayllar tayyorlanmoqda: ' + Math.floor(k * 100 / n) + '%'); bar(k * 100 / n); await new Promise((r) => setTimeout(r, 0)); }
  }
}

/* ---------- server qo'shimcha fayllari (fastdl) ----------
 * Brauzerdagi dvijok HTTP orqali yuklay olmaydi ("no servers to download ..."). Bunday fayllarni sahifa o'zi
 * ko'prik orqali (/xcws/dl) yuklaydi, FS ga yozadi, keshlaydi va serverga qayta ulanadi (retry). */
let SERVER = '', XG = null, reconnT = 0;
const reconnecting = () => Date.now() - reconnT < 5000;
const MISS = new Set(); let missTm = 0, dlBusy = false, retries = 0;
function note(t) { const n = $('note'); if (!n) return; n.textContent = t; n.style.display = t ? 'block' : 'none'; }
/* APK dagidek yuklanish ekrani: ulanishdan xaritaga kirguncha (CL_SignonReply: 2) - konsol xatolari va pushti kataklar ko'rinmaydi */
let loadOn = false, loadP = 0, loadTm = 0;
function loading(on, text, p) {
  const el = $('load'); if (!el) return;
  if (on) {
    if (!loadOn) { loadOn = true; loadP = 5; el.style.display = 'block'; clearInterval(loadTm); loadTm = setInterval(() => { if (loadP < 90) { loadP += (90 - loadP) * 0.03; $('lb').style.width = loadP + '%'; } }, 500); }
    if (text) $('lt').textContent = text;
    if (p != null) { loadP = Math.max(loadP, p); $('lb').style.width = loadP + '%'; }
  } else if (loadOn) {
    loadOn = false; clearInterval(loadTm); $('lb').style.width = '100%';
    setTimeout(() => { if (!loadOn) el.style.display = 'none'; }, 250);
  }
}
let lastShown = 0;
function onLog(t) {
  if (t.indexOf('NET_QueuePacket') >= 0) return;   // har kadrda takrorlanadigan zararsiz xabar
  if (loadOn) { const now = Date.now(); if (now - lastShown > 250) { lastShown = now; const c = String(t).replace(/\^\d/g, '').trim(); if (c) $('ll').textContent = c.slice(0, 120); } }
  if (/processing downloaded/.test(t)) loading(true, 'Server fayllari olinmoqda...', 40);
  if (/Connecting to |Commencing connection retry|reconnecting/.test(t)) loading(true, 'Serverga ulanmoqda...');
  else if (/Connection accepted|Signon|precach|Loading decals|processing downloaded/i.test(t)) loading(true, "Xarita va fayllar yuklanmoqda...", 30);
  else if (/CL_SignonReply: 2/.test(t)) { loading(false); retries = 0; if (XG) XG.Cmd_ExecuteString('cl_allowdownload 1'); setTimeout(saveDownloads, 3000); }
  else if (!dlBusy && !reconnecting() && /Server connection timed out|Connection refused|Server disconnected|kicked|banned|Bad password/i.test(t)) { loading(false); }
  let m = /Connecting to (\d{1,3}(?:\.\d{1,3}){3}:\d+)/.exec(t);
  if (m && m[1] !== SERVER) { SERVER = m[1]; retries = 0; }
  m = /no servers to download (\S+)/.exec(t);
  if (m && SERVER) { MISS.add(m[1].replace(/\.$/, '')); clearTimeout(missTm); missTm = setTimeout(fetchMissing, 1500); }
}
async function fetchMissing() {
  if (dlBusy || !MISS.size || !XG) return;
  dlBusy = true;
  const list = [...MISS]; MISS.clear();
  let cache = null; try { cache = await caches.open('xc-dl'); } catch (e) {}
  let done = 0, ok = 0;
  const work = list.slice();
  const one = async () => {
    for (let f; (f = work.shift());) {
      try {
        const r = await fetch('/xcws/dl?s=' + encodeURIComponent(SERVER) + '&f=' + encodeURIComponent(f));
        if (r.ok) {
          const b = new Uint8Array(await r.arrayBuffer());
          const path = '/xash/cstrike/downloaded/' + f;
          XG.em.FS.mkdirTree(path.slice(0, path.lastIndexOf('/')));
          XG.em.FS.writeFile(path, b);
          if (cache) cache.put('/dl/' + f, new Response(b)).catch(() => {});
          ok++;
        }
      } catch (e) {}
      done++; loading(true, 'Server fayllari yuklanmoqda: ' + done + ' / ' + list.length, 20 + 60 * done / list.length);
    }
  };
  await Promise.all([one(), one(), one(), one()]);
  dlBusy = false;
  /* "retry" yuklash navbati osilib qolsa ishlamaydi - to'liq uzilib, qaytadan ulanamiz.
   * 3-urinishda ham fayl yetmasa (serverda yo'q) - yuklashsiz kiramiz (bitta fayl o'rnida pushti katak, lekin o'yin qotmaydi) */
  loading(true, 'Yuklandi: ' + ok + ' / ' + list.length + '. Serverga qayta ulanmoqda...', 85);
  retries++;
  reconnT = Date.now();
  XG.Cmd_ExecuteString('disconnect');
  XG.Cmd_ExecuteString('fs_rescan');
  if (retries >= 3) XG.Cmd_ExecuteString('cl_allowdownload 0');
  const srv = SERVER;
  setTimeout(() => { if (XG) XG.Cmd_ExecuteString('connect ' + srv); }, 700);
}
/* o'yin o'zi (UDP orqali) yuklab olgan server fayllarini ham saqlaymiz - keyingi kirishda qayta yuklanmaydi */
async function saveDownloads() {
  if (!XG) return;
  let cache; try { cache = await caches.open('xc-dl'); } catch (e) { return; }
  const have = new Set((await cache.keys()).map((r) => decodeURIComponent(new URL(r.url).pathname)));
  const FS = XG.em.FS, root = '/xash/cstrike/downloaded';
  const walk = (dir, rel) => {
    let l = []; try { l = FS.readdir(dir); } catch (e) { return; }
    for (const n of l) {
      if (n === '.' || n === '..') continue;
      const p = dir + '/' + n, r = rel ? rel + '/' + n : n;
      let st; try { st = FS.stat(p); } catch (e) { continue; }
      if (FS.isDir(st.mode)) walk(p, r);
      else if (!have.has('/dl/' + r) && !/\.incomplete$/.test(n)) { try { cache.put('/dl/' + r, new Response(FS.readFile(p))).catch(() => {}); } catch (e) {} }
    }
  };
  walk(root, '');
}
/* oldin yuklangan server fayllari - keyingi safar darhol */
async function loadDownloads(FS) {
  try {
    const cache = await caches.open('xc-dl');
    for (const req of await cache.keys()) {
      const f = decodeURIComponent(new URL(req.url).pathname.replace(/^\/dl\//, ''));
      if (!f || f.includes('..')) continue;
      const b = new Uint8Array(await (await cache.match(req)).arrayBuffer());
      const path = '/xash/cstrike/downloaded/' + f;
      FS.mkdirTree(path.slice(0, path.lastIndexOf('/')));
      FS.writeFile(path, b);
    }
  } catch (e) {}
}
/* dvijokning "server kutubxonasi topilmadi" ogohlantirishi - onlayn o'yinga kerak emas */
const _alert = window.alert.bind(window);
window.alert = (m) => { if (/dlls\/|emscripten_wasm32|Xash Warning/.test(String(m))) { console.warn(m); return; } _alert(m); };

/* ---------- ishga tushirish ---------- */
async function start(connect) {
  $('go').disabled = true;
  try {
    const zip = await getData();
    say("O'yin yuklanmoqda...");
    const x = new Xash3D({
      canvas: $('canvas'),
      renderer: 'gl4es',
      arguments: ['-game', 'cstrike', '-dev', '2', '+con_notifytime', '0', '+touch_enable', '1', '+_vgui_menus', '0'].concat(connect ? ['+connect', connect] : []),
      libraries: { client: 'client.wasm', menu: 'libmenu.wasm', server: 'server.wasm' },
      filesMap: { 'xash.wasm': 'xash.wasm', 'filesystem_stdio.wasm': 'filesystem_stdio.wasm', 'libref_webgl2.wasm': 'libref_webgl2.wasm', 'libref_soft.wasm': 'libref_soft.wasm' },
      module: { print: (t) => onLog(t), printErr: (t) => onLog(t) },   // -dev 2: signon xabarlari kerak; konsolga chiqarilmaydi (tezlik)
    });
    const sender = new WsSender(RELAYS);
    x.net = new XcNet(sender); sender.net = x.net;
    await x.init();
    await mountZip(x.em.FS, zip, '/xash');
    await loadDownloads(x.em.FS);
    XG = x;
    x.em.FS.chdir('/xash');
    $('ui').style.display = 'none'; $('canvas').style.display = 'block';
    window.XC = x;
    try { x.main(); } catch (e) { if (e !== Infinity && e !== 'unwind') throw e; }   // Emscripten asosiy tsikli shunday "chiqadi" - bu xato emas
  } catch (e) {
    say('Xato: ' + (e && e.message || e) + '. Sahifani yangilab qayta urinib ko\'ring.');
    $('go').disabled = false;
  }
}

/* serverlar ro'yxati (X CLIENT sayti) */
fetch('https://mmb.boost-top.com/servers.php').then((r) => r.json()).then((l) => {
  const box = $('srv'); const seen = {}; const list = [];
  for (const sec of (l.sections || [])) for (const s of (sec.servers || [])) if (s.ip && !seen[s.ip] && s.online !== false) { seen[s.ip] = 1; list.push(s); }
  list.splice(12);
  for (const s of list) {
    const ip = s.ip || s.addr; if (!ip) continue;
    const a = document.createElement('button'); a.className = 'srv';
    a.textContent = (s.name || s.hostname || ip) + (s.players != null ? '  ·  ' + s.players + '/' + (s.max || s.maxplayers || 32) : '');
    a.onclick = () => start(ip);
    box.appendChild(a);
  }
}).catch(() => {});
$('go').onclick = () => start('212.47.58.89:27015');
$('lx').onclick = () => { if (XG) XG.Cmd_ExecuteString('disconnect'); loading(false); };
if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
