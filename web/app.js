(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const inr = n => '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 0 });
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const state = { packages: [], travellers: [], bookings: [], filter: 'ALL', q: '' };
  const GRADIENTS = [
    'linear-gradient(135deg,#0ea5a4,#2563eb)', 'linear-gradient(135deg,#f59e0b,#e11d48)',
    'linear-gradient(135deg,#7c3aed,#db2777)', 'linear-gradient(135deg,#16a34a,#0891b2)',
    'linear-gradient(135deg,#ea580c,#a21caf)', 'linear-gradient(135deg,#0284c7,#4f46e5)'
  ];

  async function api(path, opts = {}) {
    const res = await fetch('/api' + path, {
      method: opts.method || 'GET',
      headers: opts.body ? { 'Content-Type': 'application/json' } : undefined,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });
    let data = null;
    try { data = await res.json(); } catch { /* empty */ }
    if (!res.ok) throw new Error((data && data.error) || 'Request failed (' + res.status + ')');
    return data;
  }

  function toast(msg, err) {
    const t = document.createElement('div');
    t.className = 'toast' + (err ? ' err' : '');
    t.textContent = msg;
    $('#toasts').append(t);
    setTimeout(() => t.remove(), err ? 6000 : 3500);
  }

  async function refresh() {
    try {
      [state.packages, state.travellers, state.bookings] = await Promise.all([api('/packages'), api('/travellers'), api('/bookings')]);
      render();
    } catch (e) { toast('Could not reach the server: ' + e.message, true); }
  }

  /* ---------- tabs ---------- */
  function show(tab) {
    document.querySelectorAll('.tab').forEach(s => s.hidden = s.id !== 'tab-' + tab);
    document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('on', b.dataset.tab === tab));
    if (location.hash !== '#' + tab) history.replaceState(null, '', '#' + tab);
  }
  $('#nav').addEventListener('click', e => { const b = e.target.closest('button'); if (b) show(b.dataset.tab); });
  document.addEventListener('click', e => { const a = e.target.closest('[data-go]'); if (a) { e.preventDefault(); show(a.dataset.go); } });

  /* ---------- render ---------- */
  function render() {
    renderStats(); renderPackages(); renderBookSelects(); renderSummary(); renderBookings(); renderTravellers();
  }

  function renderStats() {
    const active = state.bookings.filter(b => b.status === 'CONFIRMED');
    const revenue = active.reduce((s, b) => s + b.total, 0);
    const seats = state.packages.reduce((s, p) => s + p.seats, 0);
    $('#stats').innerHTML = [
      [state.packages.length, 'Packages'], [seats, 'Seats open'],
      [active.length, 'Active bookings'], [inr(revenue), 'Booked value']
    ].map(([v, l]) => `<div class="stat"><b>${esc(v)}</b><span>${l}</span></div>`).join('');
  }

  const maxSeats = id => Math.max(...state.packages.map(p => p.seats), 1);
  function renderPackages() {
    if (!state.packages.length) { $('#pkgGrid').innerHTML = '<p class="empty">No packages yet.</p>'; return; }
    const cap = Math.max(...state.packages.map(p => p.seats), 20);
    $('#pkgGrid').innerHTML = state.packages.map((p, i) => {
      const pct = Math.min(100, Math.round(p.seats / cap * 100));
      const cls = p.seats === 0 ? 'out' : p.seats <= 5 ? 'low' : '';
      return `<article class="pkg">
        <div class="art" style="background:${GRADIENTS[i % GRADIENTS.length]}">${esc(p.destination)}</div>
        <div class="body">
          <h3>${esc(p.name)}</h3>
          <div class="price">${inr(p.price)} <small>per seat</small></div>
          <div><div class="meter ${cls}" aria-hidden="true"><i style="width:${pct}%"></i></div>
          <div class="seats"><span>${p.seats === 0 ? 'Sold out' : p.seats + ' seats left'}</span><span>#${p.id}</span></div></div>
          <button class="btn primary" data-book="${p.id}" ${p.seats === 0 ? 'disabled' : ''}>Book this trip</button>
        </div></article>`;
    }).join('');
  }
  $('#pkgGrid').addEventListener('click', e => {
    const b = e.target.closest('[data-book]');
    if (!b) return;
    $('#bkPackage').value = b.dataset.book;
    $('#bkSeats').value = 1;
    renderSummary(); show('book');
  });

  function renderBookSelects() {
    const tSel = $('#bkTraveller'), pSel = $('#bkPackage');
    const tv = tSel.value, pv = pSel.value;
    tSel.innerHTML = '<option value="">Select a traveller…</option>' +
      state.travellers.map(t => `<option value="${t.id}">${esc(t.name)} (#${t.id})</option>`).join('');
    pSel.innerHTML = '<option value="">Select a package…</option>' +
      state.packages.map(p => `<option value="${p.id}" ${p.seats === 0 ? 'disabled' : ''}>${esc(p.name)} — ${inr(p.price)} · ${p.seats} left</option>`).join('');
    tSel.value = tv; pSel.value = pv;
  }

  function renderSummary() {
    const p = state.packages.find(x => x.id == $('#bkPackage').value);
    const t = state.travellers.find(x => x.id == $('#bkTraveller').value);
    const n = parseInt($('#bkSeats').value, 10) || 0;
    if (!p) { $('#summary').innerHTML = '<h2>Summary</h2><p class="empty">Select a package to see the price.</p>'; return; }
    $('#summary').innerHTML = `<h2>Summary</h2><dl>
      <dt>Package</dt><dd>${esc(p.name)}</dd><dt>Destination</dt><dd>${esc(p.destination)}</dd>
      <dt>Traveller</dt><dd>${t ? esc(t.name) : '—'}</dd>
      <dt>Price per seat</dt><dd>${inr(p.price)}</dd><dt>Seats</dt><dd>${n} of ${p.seats} available</dd></dl>
      <div class="total"><span>Total</span><b>${inr(p.price * Math.max(n, 0))}</b></div>`;
  }
  ['#bkPackage', '#bkTraveller', '#bkSeats'].forEach(s => $(s).addEventListener('input', renderSummary));
  $('#seatMinus').onclick = () => { const i = $('#bkSeats'); i.value = Math.max(1, (+i.value || 1) - 1); renderSummary(); };
  $('#seatPlus').onclick = () => { const i = $('#bkSeats'); i.value = (+i.value || 0) + 1; renderSummary(); };

  $('#bookForm').addEventListener('submit', async e => {
    e.preventDefault();
    const tSel = $('#bkTraveller'), pSel = $('#bkPackage'), sIn = $('#bkSeats');
    [tSel, pSel, sIn].forEach(x => x.classList.remove('bad'));
    const seats = parseInt(sIn.value, 10);
    if (!tSel.value) { tSel.classList.add('bad'); return toast('Select a traveller.', true); }
    if (!pSel.value) { pSel.classList.add('bad'); return toast('Select a package.', true); }
    if (!(seats > 0)) { sIn.classList.add('bad'); return toast('Seats must be at least 1.', true); }
    const btn = e.submitter; btn.disabled = true;
    try {
      const r = await api('/bookings', { method: 'POST', body: { travellerId: tSel.value, packageId: pSel.value, seats } });
      toast(`Booking #${r.id} confirmed — ${inr(r.total)}`);
      sIn.value = 1; await refresh(); show('bookings');
    } catch (err) { toast(err.message, true); }
    finally { btn.disabled = false; }
  });

  function renderBookings() {
    const q = state.q.toLowerCase();
    const rows = state.bookings.filter(b => (state.filter === 'ALL' || b.status === state.filter) &&
      (!q || (b.traveller + ' ' + b.package + ' ' + b.destination + ' #' + b.id).toLowerCase().includes(q)));
    const head = '<thead><tr><th>ID</th><th>Traveller</th><th>Package</th><th class="num">Seats</th><th class="num">Total</th><th>Status</th><th></th></tr></thead>';
    $('#bkTable').innerHTML = rows.length ? head + '<tbody>' + rows.map(b => `<tr>
      <td class="id">#${b.id}</td><td>${esc(b.traveller)}</td><td>${esc(b.package)}</td>
      <td class="num">${b.seats}</td><td class="num">${inr(b.total)}</td>
      <td><span class="pill ${b.status}">${b.status[0] + b.status.slice(1).toLowerCase()}</span></td>
      <td class="num">${b.status === 'CONFIRMED' ? `<button class="btn sm ghost-danger" data-cancel="${b.id}">Cancel</button>` : ''}</td></tr>`).join('') + '</tbody>'
      : '<tbody><tr><td class="empty" colspan="7">No bookings to show.</td></tr></tbody>';
  }
  $('#statusFilter').addEventListener('click', e => {
    const b = e.target.closest('button'); if (!b) return;
    state.filter = b.dataset.f;
    document.querySelectorAll('#statusFilter button').forEach(x => x.classList.toggle('on', x === b));
    renderBookings();
  });
  $('#bkSearch').addEventListener('input', e => { state.q = e.target.value.trim(); renderBookings(); });

  $('#bkTable').addEventListener('click', async e => {
    const btn = e.target.closest('[data-cancel]'); if (!btn) return;
    const b = state.bookings.find(x => x.id == btn.dataset.cancel); if (!b) return;
    const dlg = $('#confirm');
    $('#cfTitle').textContent = `Cancel booking #${b.id}?`;
    $('#cfBody').textContent = `${b.traveller} · ${b.package} · ${b.seats} seat(s). You will refund ${inr(b.total * 0.9)} (90%) and keep a ${inr(b.total * 0.1)} cancellation charge.`;
    dlg.returnValue = '';
    dlg.showModal();
    dlg.addEventListener('close', async () => {
      if (dlg.returnValue !== 'yes') return;
      try {
        const r = await api(`/bookings/${b.id}/cancel`, { method: 'POST' });
        toast(`Booking #${r.id} cancelled. Refund ${inr(r.refund)}.`);
        refresh();
      } catch (err) { toast(err.message, true); }
    }, { once: true });
  });

  function renderTravellers() {
    const head = '<thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Email</th></tr></thead>';
    $('#trTable').innerHTML = state.travellers.length ? head + '<tbody>' + state.travellers.map(t =>
      `<tr><td class="id">#${t.id}</td><td>${esc(t.name)}</td><td>${esc(t.phone) || '—'}</td><td>${esc(t.email) || '—'}</td></tr>`).join('') + '</tbody>'
      : '<tbody><tr><td class="empty" colspan="4">No travellers registered yet.</td></tr></tbody>';
  }

  $('#regForm').addEventListener('submit', async e => {
    e.preventDefault();
    const name = $('#rgName'); name.classList.remove('bad');
    if (!name.value.trim()) { name.classList.add('bad'); return toast('Name is required.', true); }
    const btn = e.submitter; btn.disabled = true;
    try {
      const t = await api('/travellers', { method: 'POST', body: { name: name.value, phone: $('#rgPhone').value, email: $('#rgEmail').value } });
      toast(`${t.name} registered — Traveller ID ${t.id}`);
      e.target.reset(); await refresh();
      $('#bkTraveller').value = t.id; renderSummary();
    } catch (err) { toast(err.message, true); }
    finally { btn.disabled = false; }
  });

  const initial = location.hash.slice(1);
  show(['packages', 'book', 'bookings', 'travellers'].includes(initial) ? initial : 'packages');
  refresh();
})();
