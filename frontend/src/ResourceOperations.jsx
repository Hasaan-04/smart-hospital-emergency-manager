import { useEffect, useState } from 'react'

const initialDoctor = {
  employeeCode: '', fullName: '', specialization: '',
  departmentId: '', shiftStart: '08:00', shiftEnd: '16:00'
}
const initialAmbulance = { vehicleNumber: '', driverName: '' }
const initialEquipment = {
  assetCode: '', name: '', category: 'VENTILATOR',
  departmentId: '', roomId: ''
}

export default function ResourceOperations({ hospitalId }) {
  const [departments, setDepartments] = useState([])
  const [rooms, setRooms] = useState([])
  const [doctors, setDoctors] = useState([])
  const [ambulances, setAmbulances] = useState([])
  const [equipment, setEquipment] = useState([])
  const [doctor, setDoctor] = useState(initialDoctor)
  const [ambulance, setAmbulance] = useState(initialAmbulance)
  const [asset, setAsset] = useState(initialEquipment)
  const [movement, setMovement] = useState({ id: '', departmentId: '', roomId: '', note: '' })
  const [history, setHistory] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const base = `/api/hospitals/${hospitalId}`

  async function request(path, options = {}) {
    const res = await fetch(path, options)
    if (!res.ok) {
      let details = {}
      try { details = await res.json() } catch { /* response was not JSON */ }
      throw new Error(details.detail || details.message || `HTTP ${res.status}`)
    }
    return res.json()
  }

  function send(path, method, obj) {
    return request(path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(obj)
    })
  }

  async function reload() {
    if (!hospitalId) return
    const [d, r, ds, a, e] = await Promise.all([
      request(`${base}/departments`),
      request(`${base}/rooms`),
      request(`${base}/resources/doctors`),
      request(`${base}/resources/ambulances`),
      request(`${base}/resources/equipment`)
    ])
    setDepartments(d)
    setRooms(r)
    setDoctors(ds)
    setAmbulances(a)
    setEquipment(e)
  }

  useEffect(() => {
    let active = true
    if (!hospitalId) {
      setDepartments([]); setRooms([]); setDoctors([])
      setAmbulances([]); setEquipment([]); setHistory([])
      return
    }
    Promise.all([
      request(`${base}/departments`),
      request(`${base}/rooms`),
      request(`${base}/resources/doctors`),
      request(`${base}/resources/ambulances`),
      request(`${base}/resources/equipment`)
    ]).then(([d, r, ds, a, e]) => {
      if (!active) return
      setDepartments(d); setRooms(r); setDoctors(ds)
      setAmbulances(a); setEquipment(e)
    }).catch(err => { if (active) setError(err.message) })
    return () => { active = false }
  }, [hospitalId])

  async function perform(action, successText) {
    setError(''); setMessage('')
    try { await action(); await reload(); setMessage(successText) }
    catch (err) { setError(err.message) }
  }

  const departmentOptions = departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)
  const selectedAssetRooms = rooms.filter(r => String(r.departmentId) === String(asset.departmentId))
  const selectedMoveRooms = rooms.filter(r => String(r.departmentId) === String(movement.departmentId))

  if (!hospitalId) return <section className="card"><h2>Additional Resources</h2><p>Select a hospital first.</p></section>

  return (
    <section className="resource-operations">
      <h2>Doctors, Ambulances & Medical Equipment</h2>
      <p>Session 4 functional interface (we will redesign the GUI after backend completion).</p>
      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}
      <button type="button" onClick={() => perform(async () => {}, 'Lists refreshed')}>Refresh lists</button>

      <section className="card">
        <h2>1. Register Doctor</h2>
        <form onSubmit={e => { e.preventDefault(); perform(async () => {
          await send(`${base}/resources/doctors`, 'POST', {
            ...doctor, departmentId: Number(doctor.departmentId)
          }); setDoctor(initialDoctor)
        }, 'Doctor registered') }}>
          <input placeholder="Employee Code (D-001)" required value={doctor.employeeCode}
            onChange={e => setDoctor({ ...doctor, employeeCode: e.target.value })} />
          <input placeholder="Doctor Name" required value={doctor.fullName}
            onChange={e => setDoctor({ ...doctor, fullName: e.target.value })} />
          <input placeholder="Specialization (e.g. Cardiology)" required value={doctor.specialization}
            onChange={e => setDoctor({ ...doctor, specialization: e.target.value })} />
          <label>Department</label>
          <select required value={doctor.departmentId}
            onChange={e => setDoctor({ ...doctor, departmentId: e.target.value })}>
            <option value="">Select Department</option>{departmentOptions}
          </select>
          <label>Shift Start</label>
          <input type="time" required value={doctor.shiftStart}
            onChange={e => setDoctor({ ...doctor, shiftStart: e.target.value })} />
          <label>Shift End</label>
          <input type="time" required value={doctor.shiftEnd}
            onChange={e => setDoctor({ ...doctor, shiftEnd: e.target.value })} />
          <button type="submit" disabled={!departments.length}>Add Doctor</button>
        </form>
        <h3>Registered Doctors</h3>
        {doctors.map(d => <div className="resource-item" key={d.id}>
          <strong>{d.fullName} — {d.specialization}</strong>
          <span>{d.employeeCode} | {d.departmentName} | Shift {d.shiftStart}–{d.shiftEnd}</span>
          <span>Status: {d.status}</span>
          {d.status !== 'BUSY' && <button type="button" onClick={() => perform(
            () => send(`${base}/resources/doctors/${d.id}/status`, 'PATCH', {
              status: d.status === 'AVAILABLE' ? 'OFF_DUTY' : 'AVAILABLE'
            }), 'Doctor status updated'
          )}>{d.status === 'AVAILABLE' ? 'Mark Off Duty' : 'Mark Available'}</button>}
        </div>)}
      </section>

      <section className="card">
        <h2>2. Register Ambulance</h2>
        <form onSubmit={e => { e.preventDefault(); perform(async () => {
          await send(`${base}/resources/ambulances`, 'POST', ambulance)
          setAmbulance(initialAmbulance)
        }, 'Ambulance registered') }}>
          <input required placeholder="Vehicle Number (AMB-01)" value={ambulance.vehicleNumber}
            onChange={e => setAmbulance({ ...ambulance, vehicleNumber: e.target.value })} />
          <input required placeholder="Driver Name" value={ambulance.driverName}
            onChange={e => setAmbulance({ ...ambulance, driverName: e.target.value })} />
          <button type="submit">Add Ambulance</button>
        </form>
        <h3>Registered Ambulances</h3>
        {ambulances.map(a => <div className="resource-item" key={a.id}>
          <strong>{a.vehicleNumber}</strong><span>Driver: {a.driverName} | {a.status}</span>
          <select aria-label={`Change status of ${a.vehicleNumber}`} value={a.status}
            onChange={e => perform(
              () => send(`${base}/resources/ambulances/${a.id}/status`, 'PATCH', { status: e.target.value }),
              'Ambulance status updated'
            )}>
            <option value="AVAILABLE">Available</option>
            <option value="DISPATCHED">Dispatched</option>
            <option value="MAINTENANCE">Maintenance</option>
          </select>
        </div>)}
      </section>

      <section className="card">
        <h2>3. Register Medical Equipment</h2>
        <form onSubmit={e => { e.preventDefault(); perform(async () => {
          await send(`${base}/resources/equipment`, 'POST', {
            ...asset, departmentId: Number(asset.departmentId),
            roomId: asset.roomId ? Number(asset.roomId) : null
          }); setAsset(initialEquipment)
        }, 'Medical equipment registered') }}>
          <input required placeholder="Asset Code (VENT-001)" value={asset.assetCode}
            onChange={e => setAsset({ ...asset, assetCode: e.target.value })} />
          <input required placeholder="Equipment Name" value={asset.name}
            onChange={e => setAsset({ ...asset, name: e.target.value })} />
          <label>Equipment Type</label>
          <select value={asset.category} onChange={e => setAsset({ ...asset, category: e.target.value })}>
            <option value="VENTILATOR">Ventilator</option>
            <option value="OXYGEN">Oxygen Support Unit</option>
            <option value="MONITOR">Patient Monitor</option>
            <option value="OTHER">Other</option>
          </select>
          <label>Department</label>
          <select required value={asset.departmentId} onChange={e => setAsset({
            ...asset, departmentId: e.target.value, roomId: ''
          })}><option value="">Select Department</option>{departmentOptions}</select>
          <label>Room (optional)</label>
          <select value={asset.roomId} onChange={e => setAsset({ ...asset, roomId: e.target.value })}>
            <option value="">No room assigned</option>
            {selectedAssetRooms.map(r => <option key={r.id} value={r.id}>{r.number}</option>)}
          </select>
          <button type="submit" disabled={!departments.length}>Add Equipment</button>
        </form>
        <h3>Registered Equipment</h3>
        {equipment.map(e => <div className="resource-item" key={e.id}>
          <strong>{e.assetCode} — {e.name}</strong>
          <span>{e.category} | {e.departmentName}{e.roomNumber ? ` / ${e.roomNumber}` : ''} | {e.status}</span>
          {e.status !== 'IN_USE' && <button type="button" onClick={() => perform(
            () => send(`${base}/resources/equipment/${e.id}/status`, 'PATCH', {
              status: e.status === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE'
            }), 'Equipment status updated'
          )}>{e.status === 'AVAILABLE' ? 'Mark Maintenance' : 'Mark Available'}</button>}
          <button type="button" onClick={() => {
            setMovement({ id: String(e.id), departmentId: String(e.departmentId),
              roomId: e.roomId ? String(e.roomId) : '', note: '' })
          }}>Move Equipment</button>
          <button type="button" onClick={() => perform(async () => {
            const data = await request(`${base}/resources/equipment/${e.id}/history`)
            setHistory(data)
          }, `History for ${e.assetCode}`)}>Show History</button>
        </div>)}

        {movement.id && <form onSubmit={e => { e.preventDefault(); perform(async () => {
          await send(`${base}/resources/equipment/${movement.id}/move`, 'PATCH', {
            departmentId: Number(movement.departmentId),
            roomId: movement.roomId ? Number(movement.roomId) : null,
            note: movement.note
          }); setMovement({ id: '', departmentId: '', roomId: '', note: '' })
        }, 'Equipment moved') }}>
          <h3>Move Equipment #{movement.id}</h3>
          <label>Destination department</label>
          <select required value={movement.departmentId} onChange={e => setMovement({
            ...movement, departmentId: e.target.value, roomId: ''
          })}><option value="">Choose department</option>{departmentOptions}</select>
          <label>Destination room (optional)</label>
          <select value={movement.roomId} onChange={e => setMovement({ ...movement, roomId: e.target.value })}>
            <option value="">No room assigned</option>
            {selectedMoveRooms.map(r => <option key={r.id} value={r.id}>{r.number}</option>)}
          </select>
          <input maxLength={150} placeholder="Move reason (optional)" value={movement.note}
            onChange={e => setMovement({ ...movement, note: e.target.value })} />
          <button type="submit">Confirm Move</button>
          <button type="button" onClick={() => setMovement({ id: '', departmentId: '', roomId: '', note: '' })}>Cancel</button>
        </form>}

        {history.length > 0 && <div><h3>Equipment History (last selected)</h3>
          {history.map(h => <div className="resource-item" key={h.id}>
            <strong>{h.eventType} — {h.occurredAt}</strong><span>{h.description}</span>
          </div>)}
        </div>}
      </section>
    </section>
  )
}
