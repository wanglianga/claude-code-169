// 四端共享：API 封装、登录态、提示与渲染工具
const API = '/api';

function getSession() {
  const raw = sessionStorage.getItem('binroom_session');
  return raw ? JSON.parse(raw) : null;
}
function setSession(s) {
  sessionStorage.setItem('binroom_session', JSON.stringify(s));
}
function clearSession() {
  sessionStorage.removeItem('binroom_session');
}

async function api(path, options = {}) {
  const session = getSession();
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (session && session.token) {
    headers['Authorization'] = 'Bearer ' + session.token;
  }
  const res = await fetch(API + path, { ...options, headers });
  const text = await res.text();
  let data = text ? JSON.parse(text) : {};
  if (!res.ok) {
    throw new Error(data.message || ('请求失败 ' + res.status));
  }
  return data;
}

function toast(msg, isErr) {
  let el = document.querySelector('.toast');
  if (!el) {
    el = document.createElement('div');
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.className = 'toast show' + (isErr ? ' err' : '');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.className = 'toast', 2600);
}

function requireRole(role) {
  const s = getSession();
  if (!s || s.role !== role) {
    location.href = '/index.html';
    return null;
  }
  return s;
}

function mountTopbar(title) {
  const s = getSession();
  const roleLabels = {
    RESIDENT: '居民端', PROPERTY: '物业端', SANITATION: '环卫端', STREET: '街道端', ADMIN: '管理端'
  };
  document.body.insertAdjacentHTML('afterbegin', `
    <header class="topbar">
      <h1>♻️ ${title}</h1>
      <span class="spacer"></span>
      <span class="who">${roleLabels[s.role]} · ${s.displayName || ''}</span>
      <button onclick="logout()">退出</button>
    </header>`);
}
function logout() {
  clearSession();
  location.href = '/index.html';
}

const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : '—';

function stateTag(state, label) {
  return `<span class="tag ${state}">${esc(label || state)}</span>`;
}

function fillBar(pct) {
  const cls = pct >= 90 ? 'hi' : pct >= 60 ? 'mid' : '';
  return `<div style="display:flex;align-items:center;gap:6px">
    <div class="fill-bar" style="flex:1"><i class="${cls}" style="width:${Math.min(pct, 100)}%"></i></div>
    <span class="muted" style="width:38px;text-align:right">${pct}%</span>
  </div>`;
}
