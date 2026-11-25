/* X CLIENT Web: CS 1.6 brauzerda (iPhone Safari, Android, kompyuter).
 * Dvijok: xash3d-fwgs (WebAssembly, MIT), o'yin: bizning cs16 client (WebAssembly).
 * O'yin fayllari bir marta yuklanadi va brauzer keshida (Cache Storage) saqlanadi.
 * Tarmoq: brauzer UDP yubora olmaydi - paketlar WebSocket orqali ws_relay.py ga, u serverga UDP qilib yuboradi. */
import { Xash3D, Net } from 'xash3d-fwgs';
import { inflateSync } from 'fflate';

const Q = new URLSearchParams(location.search);
const RELAY = Q.get('relay') || 'wss://mmb.boost-top.com:27080/';
const DATA_URL = 'xc_web_data.zip';
const DATA_VER = '__DATA_VER__';                 // CI almashtiradi - arxiv yangilansa qayta yuklanadi
const $ = (id) => document.getElementById(id);
const say = (t) => { $('st').textContent = t; };
const bar = (p) => { $('bar').style.width = Math.max(0, Math.min(100, p)) + '%'; };

/* ---------- tarmoq: WebSocket ko'prik ---------- */
class WsSender {
  constructor(url) { this.url = url; this.q = []; this.net = null; this.connect(); }
  connect() {
    const ws = new WebSocket(this.url);
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => { for (const b of this.q) ws.send(b); this.q = []; };
    ws.onmessage = (e) => {
      const d = new Uint8Array(e.data);
      if (d.length < 6 || !this.net) return;
      this.net.incoming.push({ ip: [d[0], d[1], d[2], d[3]], port: (d[4] << 8) | d[5], data: d.subarray(6) });
    };
    ws.onclose = () => setTimeout(() => this.connect(), 1500);
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

/* ---------- ishga tushirish ---------- */
async function start(connect) {
  $('go').disabled = true;
  try {
    const zip = await getData();
    say("O'yin yuklanmoqda...");
    const x = new Xash3D({
      canvas: $('canvas'),
      renderer: 'gl4es',
      arguments: ['-game', 'cstrike', '+touch_enable', '1', '+_vgui_menus', '0'].concat(connect ? ['+connect', connect] : []),
      libraries: { client: 'client.wasm', menu: 'libmenu.wasm', server: 'server.wasm' },
      filesMap: { 'xash.wasm': 'xash.wasm', 'filesystem_stdio.wasm': 'filesystem_stdio.wasm', 'libref_webgl2.wasm': 'libref_webgl2.wasm', 'libref_soft.wasm': 'libref_soft.wasm' },
      module: { print: (t) => console.log(t), printErr: (t) => console.warn(t) },
    });
    const sender = new WsSender(RELAY);
    x.net = new XcNet(sender); sender.net = x.net;
    await x.init();
    await mountZip(x.em.FS, zip, '/xash');
    x.em.FS.chdir('/xash');
    $('ui').style.display = 'none'; $('canvas').style.display = 'block';
    x.main();
    window.XC = x;
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
if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
