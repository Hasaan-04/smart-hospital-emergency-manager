import { useEffect, useState } from 'react'

const URGENCIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

const blankForm = () => ({
  patientName: '',
  age: '',
  chiefComplaint: '',
  urgency: 'MEDIUM',
  arrivalStatus: 'ARRIVED',
  estimatedArrivalAt: '',
  ambulanceId: '',
  requestedBedType: '',
  requestedSpecialization: '',
  oxygenNeeded: false,
  ventilatorNeeded: false
})

const displayDate = value =>
  value ? new Date(value).toLocaleString() : '—'

function minutesWaiting(value) {
  if (!value) return '—'
  const mins = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000))
  return mins < 60 ? `${mins} min` : `${Math.floor(mins / 60)}h ${mins % 60}m`
}

export default function PatientCases({ hospitalId }) {
  const [form, setForm] = useState(blankForm)
  const [ambulances, setAmbulances] = useState([])
  const [queue, setQueue] = useState([])
  const [incoming, setIncoming] = useState([])
  const [cases, setCases] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [working, setWorking] = useState(false)
  const [, setClock] = useState(0)

  const base = `/api/hospitals/${hospitalId}`

  useEffect(() => {
    const timer = setInterval(() => setClock(x => x + 1), 60000)
    return () => clearInterval(timer)
  }, [])

  async function request(path, options) {
    const response = await fetch(path, options)
    if (!response.ok) {
      let details = {}
      try { details = await response.json() } catch { /* not JSON */ }
      throw new Error(details.detail || details.message || `Request failed (HTTP ${response.status})`)
    }
    return response.json()
  }

  async function reload() {
    if (!hospitalId) return
    const [newQueue, newIncoming, newCases, newAmbulances] = await Promise.all([
      request(`${base}/cases/queue`),
      request(`${base}/cases/incoming`),
      request(`${base}/cases`),
      request(`${base}/resources/ambulances`)
    ])
    setQueue(newQueue)
    setIncoming(newIncoming)
    setCases(newCases)
    setAmbulances(newAmbulances)
  }

  useEffect(() => {
    let active = true
    setQueue([])
    setIncoming([])
    setCases([])
    setAmbulances([])
    setMessage('')
    setError('')
    setForm(blankForm())

    if (!hospitalId) return () => { active = false }

    Promise.all([
      request(`${base}/cases/queue`),
      request(`${base}/cases/incoming`),
      request(`${base}/cases`),
      request(`${base}/resources/ambulances`)
    ]).then(([q, i, all, a]) => {
      if (active) {
        setQueue(q); setIncoming(i); setCases(all); setAmbulances(a)
      }
    }).catch(err => { if (active) setError(err.message) })

    return () => { active = false }
  }, [hospitalId])

  async function run(action, success) {
    setError('')
    setMessage('')
    setWorking(true)
    try {
      await action()
      await reload()
      setMessage(success)
    } catch (err) {
      setError(err.message)
    } finally {
      setWorking(false)
    }
  }

  function update(name, value) {
    setForm(current => ({ ...current, [name]: value }))
  }

  function submitCase(event) {
    event.preventDefault()
    run(async () => {
      const body = {
        ...form,
        age: Number(form.age),
        ambulanceId: form.ambulanceId ? Number(form.ambulanceId) : null,
        requestedBedType: form.requestedBedType || null,
        requestedSpecialization: form.requestedSpecialization || null,
        estimatedArrivalAt: form.arrivalStatus === 'PRE_ARRIVAL'
          ? new Date(form.estimatedArrivalAt).toISOString()
          : null
      }
      await request(`${base}/cases`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      setForm(blankForm())
    }, 'Emergency case registered successfully')
  }

  function setPriority(caseId, urgency) {
    run(async () => {
      await request(`${base}/cases/${caseId}/priority`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ urgency })
      })
    }, 'Patient priority updated. Queue reordered.')
  }

  function markArrived(caseId) {
    run(async () => {
      await request(`${base}/cases/${caseId}/arrive`, { method: 'PATCH' })
    }, 'Patient arrival confirmed. Case moved into waiting queue.')
  }

  if (!hospitalId) {
    return <section className="card"><h2>Emergency Patients</h2><p>Register a hospital first.</p></section>
  }

  return (
    <div className="patient-management">
      <h2>Emergency Patient Registration & Priority Queue</h2>
      <p>
        Session 5 functional interface. Priority must be assigned by qualified clinical staff;
        user permissions will be enforced when authentication is implemented.
      </p>

      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}
      <button type="button" disabled={working} onClick={() => run(async () => {}, 'Lists refreshed')}>
        Refresh Patient Lists
      </button>

      <section className="card">
        <h2>1. Register Emergency Case</h2>
        <form onSubmit={submitCase}>
          <label>Patient Name (use fictional demo data)</label>
          <input required maxLength="120" value={form.patientName}
            onChange={e => update('patientName', e.target.value)} placeholder="e.g. Demo Patient A" />

          <label>Age</label>
          <input required type="number" min="0" max="125" value={form.age}
            onChange={e => update('age', e.target.value)} placeholder="e.g. 45" />

          <label>Chief Complaint / Reason for Emergency</label>
          <input required maxLength="500" value={form.chiefComplaint}
            onChange={e => update('chiefComplaint', e.target.value)}
            placeholder="e.g. Breathing difficulty (demo text)" />

          <label>Priority (clinician-assigned)</label>
          <select value={form.urgency} onChange={e => update('urgency', e.target.value)}>
            {URGENCIES.map(u => <option key={u} value={u}>{u}</option>)}
          </select>

          <label>Patient Arrival Type</label>
          <select value={form.arrivalStatus} onChange={e => update('arrivalStatus', e.target.value)}>
            <option value="ARRIVED">Already Arrived</option>
            <option value="PRE_ARRIVAL">Pre-arrival / Incoming</option>
          </select>

          {form.arrivalStatus === 'PRE_ARRIVAL' && <>
            <label>Estimated Arrival Time (your local time)</label>
            <input type="datetime-local" required value={form.estimatedArrivalAt}
              onChange={e => update('estimatedArrivalAt', e.target.value)} />
          </>}

          <label>Associated Ambulance (optional)</label>
          <select value={form.ambulanceId} onChange={e => update('ambulanceId', e.target.value)}>
            <option value="">No ambulance / Not yet known</option>
            {ambulances.filter(a => a.status !== 'MAINTENANCE').map(a => (
              <option key={a.id} value={a.id}>
                {a.vehicleNumber} — {a.status}
              </option>
            ))}
          </select>
          <p>Linking an ambulance does not automatically dispatch or release it. Use Ambulance Management for that.</p>

          <h3>Staff-Entered Resource Requirements</h3>
          <label>Required Bed Type (optional)</label>
          <select value={form.requestedBedType} onChange={e => update('requestedBedType', e.target.value)}>
            <option value="">Not specified</option>
            <option value="GENERAL">General</option>
            <option value="EMERGENCY">Emergency</option>
            <option value="ICU">ICU</option>
          </select>

          <label>Requested Doctor Specialization (optional)</label>
          <input maxLength="100" placeholder="e.g. Cardiology"
            value={form.requestedSpecialization}
            onChange={e => update('requestedSpecialization', e.target.value)} />

          <label>
            <input type="checkbox" style={{ width: "auto" }} checked={form.oxygenNeeded}
              onChange={e => update('oxygenNeeded', e.target.checked)} />
            {' '}Oxygen support required
          </label>
          <label>
            <input type="checkbox" style={{ width: "auto" }} checked={form.ventilatorNeeded}
              onChange={e => update('ventilatorNeeded', e.target.checked)} />
            {' '}Ventilator required
          </label>
          <p>These fields will be used by our Smart Allocation Engine in a later session.</p>

          <button type="submit" disabled={working}>Register Emergency Case</button>
        </form>
      </section>

      <section className="card">
        <h2>2. Arrived Patients — Live Priority Queue</h2>
        <p>Ordered by Critical → High → Medium → Low, then earliest actual arrival within the same priority.</p>
        {queue.length === 0 ? <p>No arrived patients are currently waiting.</p> : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead><tr>
                <th>Queue #</th><th>Case / Patient</th><th>Priority</th>
                <th>Arrival</th><th>Waiting</th><th>Change Priority</th>
              </tr></thead>
              <tbody>
                {queue.map((c, index) => <tr key={c.id}>
                  <td>{index + 1}</td>
                  <td>#{c.id} — {c.patientName}<div>{c.chiefComplaint}</div></td>
                  <td><strong>{c.urgency}</strong></td>
                  <td>{displayDate(c.arrivedAt)}</td>
                  <td>{minutesWaiting(c.arrivedAt)}</td>
                  <td><select disabled={working} value={c.urgency}
                    onChange={e => setPriority(c.id, e.target.value)}>
                    {URGENCIES.map(u => <option key={u} value={u}>{u}</option>)}
                  </select></td>
                </tr>)}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <h2>3. Incoming Patients — Pre-arrival</h2>
        <p>Incoming patients are shown separately until staff confirm their physical arrival.</p>
        {incoming.length === 0 ? <p>No incoming cases.</p> : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead><tr>
                <th>Case / Patient</th><th>Priority</th><th>ETA</th>
                <th>Ambulance</th><th>Priority Update</th><th>Action</th>
              </tr></thead>
              <tbody>
                {incoming.map(c => <tr key={c.id}>
                  <td>#{c.id} — {c.patientName}</td>
                  <td><strong>{c.urgency}</strong></td>
                  <td>{displayDate(c.estimatedArrivalAt)}</td>
                  <td>{c.ambulanceNumber || 'Not linked'}</td>
                  <td><select disabled={working} value={c.urgency}
                    onChange={e => setPriority(c.id, e.target.value)}>
                    {URGENCIES.map(u => <option key={u} value={u}>{u}</option>)}
                  </select></td>
                  <td><button disabled={working} type="button" onClick={() => markArrived(c.id)}>
                    Confirm Arrival
                  </button></td>
                </tr>)}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <h2>4. All Registered Cases</h2>
        <p>Total cases: {cases.length} · Waiting at hospital: {queue.length} · Incoming: {incoming.length}</p>
        {cases.length === 0 ? <p>No emergency cases registered.</p> : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead><tr>
                <th>ID</th><th>Patient</th><th>Urgency</th>
                <th>Arrival Status</th><th>Case Status</th><th>Registered</th>
              </tr></thead>
              <tbody>
                {cases.map(c => <tr key={c.id}>
                  <td>#{c.id}</td><td>{c.patientName}</td><td>{c.urgency}</td>
                  <td>{c.arrivalStatus}</td><td>{c.caseStatus}</td>
                  <td>{displayDate(c.registeredAt)}</td>
                </tr>)}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
