import { useEffect, useState } from "react";

// Session 7: read-only dashboard. Updates every 5 seconds through HTTP polling.
// No new libraries or WebSockets are needed for this prototype.
const POLL_MS = 5000;
const priorityValues = ["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"];

function niceDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString();
}

function priorityStyle(level) {
  const styles = {
    CRITICAL: { background: "#fee2e2", color: "#991b1b" },
    HIGH: { background: "#ffedd5", color: "#9a3412" },
    MEDIUM: { background: "#fef9c3", color: "#854d0e" },
    LOW: { background: "#dcfce7", color: "#166534" },
  };
  return styles[level] || { background: "#f1f5f9", color: "#334155" };
}

const ui = {
  section: {
    background: "#fbf7ee",
    borderRadius: 18,
    padding: 20,
    marginTop: 20,
  },
  stats: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(145px, 1fr))",
    gap: 12,
  },
  stat: {
    border: "1px solid #e4e3d7",
    borderRadius: 10,
    padding: 14,
    background: "#f1f0e7",
  },
  value: { fontSize: 26, fontWeight: 700, color: "#526c43", display: "block" },
  muted: { color: "#8a9384", fontSize: 13 },
  tag: { padding: "3px 8px", borderRadius: 30, fontWeight: 700, fontSize: 11 },
  row: { padding: "12px 0", borderBottom: "1px solid #e6e3d9" },
  filters: { display: "flex", gap: 10, flexWrap: "wrap", margin: "14px 0" },
  notice: {
    padding: 12,
    borderRadius: 8,
    marginBottom: 10,
    background: "#f9eee0",
    color: "#99623d",
  },
};

function Metric({ label, value }) {
  return (
    <div style={ui.stat}>
      <span style={ui.value}>{value ?? 0}</span>
      <span style={ui.muted}>{label}</span>
    </div>
  );
}

function ResourceTile({ label, data }) {
  if (!data) return null;
  return (
    <div style={ui.stat}>
      <h3 style={{ margin: "0 0 8px" }}>{label}</h3>
      <p style={{ margin: "6px 0" }}>
        Assignable now: <strong>{data.available}</strong> / {data.total}
      </p>
      <div className="availability-track" role="img" aria-label={`${data.available} of ${data.total} ${label.toLowerCase()} assignable`}>
        <div style={{ width: `${data.total ? Math.min(100, Math.max(0, (data.available / data.total) * 100)) : 0}%` }} />
      </div>
      <div style={{ ...ui.muted, lineHeight: 1.8 }}>
        <div>In use / dispatched: {data.engaged}</div>
        <div>Unavailable: {data.unavailable}</div>
        {data.held > 0 && <div>Previously held: {data.held}</div>}
        {data.offShift > 0 && <div>Outside shift: {data.offShift}</div>}
      </div>
    </div>
  );
}

function PatientRow({ patient, incoming = false }) {
  return (
    <div style={ui.row}>
      <div
        style={{
          display: "flex",
          gap: 8,
          justifyContent: "space-between",
          flexWrap: "wrap",
        }}
      >
        <strong>
          #{patient.id} — {patient.patientName}
        </strong>
        <span style={{ ...ui.tag, ...priorityStyle(patient.urgency) }}>
          {patient.urgency}
        </span>
      </div>
      <div style={{ ...ui.muted, marginTop: 5 }}>
        {patient.chiefComplaint || "No complaint entered"}
      </div>
      <div style={{ ...ui.muted, marginTop: 5 }}>
        {incoming
          ? `Expected arrival: ${niceDate(patient.estimatedArrivalAt)}`
          : `Waiting: ${patient.waitMinutes ?? 0} minute(s) · Arrived: ${niceDate(patient.arrivedAt)}`}
      </div>
      <div style={{ ...ui.muted, marginTop: 5 }}>
        Requests:{" "}
        {[
          patient.requestedBedType && `${patient.requestedBedType} bed`,
          patient.requestedSpecialization &&
            `${patient.requestedSpecialization} doctor`,
          patient.oxygenNeeded && "Oxygen",
          patient.ventilatorNeeded && "Ventilator",
        ]
          .filter(Boolean)
          .join(" · ") || "Not yet specified"}
      </div>
    </div>
  );
}

export default function EmergencyDashboard({ hospitalId, mode = "overview" }) {
  const [snapshot, setSnapshot] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [query, setQuery] = useState("");
  const [priority, setPriority] = useState("ALL");

  useEffect(() => {
    let mounted = true;
    Promise.resolve().then(() => {
      if (!mounted) return;
      setSnapshot(null);
      setError("");
      setLoading(Boolean(hospitalId));
    });
    if (!hospitalId) return () => { mounted = false; };

    let inFlight = false;

    async function update() {
      if (inFlight) return;
      inFlight = true;
      try {
        const response = await fetch(`/api/hospitals/${hospitalId}/dashboard`, {
          cache: "no-store",
        });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(
            body.detail ||
              body.message ||
              `Dashboard request failed (${response.status})`,
          );
        }
        if (mounted) {
          setSnapshot(body);
          setError("");
        }
      } catch (e) {
        if (mounted) setError(e.message);
      } finally {
        inFlight = false;
        if (mounted) setLoading(false);
      }
    }

    update();
    const timer = setInterval(update, POLL_MS);
    return () => {
      mounted = false;
      clearInterval(timer);
    };
  }, [hospitalId, refreshKey]);

  function matches(patient) {
    const term = query.toLowerCase().trim();
    const searchMatch =
      !term ||
      patient.patientName.toLowerCase().includes(term) ||
      String(patient.id).includes(term) ||
      (patient.chiefComplaint || "").toLowerCase().includes(term);
    return searchMatch && (priority === "ALL" || patient.urgency === priority);
  }

  if (!hospitalId)
    return (
      <section className="card">
        <h2>Emergency Command Center</h2>
        <p>Please select a hospital first.</p>
      </section>
    );

  const summary = snapshot?.summary;
  const filteredWaiting = (snapshot?.waitingQueue || []).filter(matches);
  const filteredIncoming = (snapshot?.incomingCases || []).filter(matches);

  return (
    <div className="resource-management" style={{ marginTop: 28 }}>
      <section className="card">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: 10,
          }}
        >
          <div>
            <h2 style={{ marginTop: 0 }}>{mode === "alerts" ? "Live Alerts & Shortages" : "Live Operations Overview"}</h2>
            <p style={ui.muted}>
              {snapshot?.hospitalName || "Hospital"} · Automatic refresh every 5
              seconds
            </p>
          </div>
          <button type="button" onClick={() => setRefreshKey((x) => x + 1)}>
            Refresh Now
          </button>
        </div>
        <p style={ui.muted}>
          Last update:{" "}
          {snapshot ? niceDate(snapshot.generatedAt) : "Waiting..."} · Demo data
          only · No clinical decisions are made automatically.
        </p>
        {loading && !snapshot && <p>Loading emergency dashboard...</p>}
        {error && <div className="error">{error}</div>}

        {snapshot && mode !== "alerts" && (
          <>
            <h3>Emergency Overview</h3>
            <div style={ui.stats}>
              <Metric
                label="Total Registered Cases"
                value={summary.totalCases}
              />
              <Metric label="Waiting in Hospital" value={summary.waiting} />
              <Metric label="Pre-arrival Cases" value={summary.incoming} />
              <Metric label="Under Treatment" value={summary.underTreatment} />
              <Metric
                label="Critical Waiting"
                value={summary.criticalWaiting}
              />
              <Metric
                label="Cases with Shortages"
                value={summary.shortageCases}
              />
            </div>

            <h3 style={{ marginTop: 25 }}>Resource Availability</h3>
            <p style={ui.muted}>
              "Assignable now" excludes occupied, maintenance, off-shift, or
              temporarily held resources.
            </p>
            <div style={ui.stats}>
              <ResourceTile label="Beds" data={snapshot.resources.beds} />
              <ResourceTile label="Doctors" data={snapshot.resources.doctors} />
              <ResourceTile
                label="Ambulances"
                data={snapshot.resources.ambulances}
              />
              <ResourceTile
                label="Equipment"
                data={snapshot.resources.equipment}
              />
            </div>
          </>
        )}
      </section>

      {snapshot && (
        <>
          <section style={ui.section}>
            <h2>
              Emergency Alerts & Resource Shortages ({snapshot.alerts.length})
            </h2>
            <p style={ui.muted}>
              Warnings are generated from staff-entered urgency and the Session
              6 allocation rules.
            </p>
            {snapshot.alerts.length === 0 ? (
              <p>No alerts at this moment.</p>
            ) : (
              snapshot.alerts.map((alert, i) => (
                <div
                  style={{
                    ...ui.notice,
                    ...(alert.severity === "CRITICAL"
                      ? { background: "#fee2e2", color: "#991b1b" }
                      : {}),
                  }}
                  key={`${alert.caseId}-${alert.type}-${i}`}
                >
                  <strong>
                    {alert.type} · Case #{alert.caseId} · {alert.patientName}
                  </strong>
                  <div style={{ marginTop: 5 }}>{alert.message}</div>
                </div>
              ))
            )}
          </section>

          {mode !== "alerts" && <section style={ui.section}>
            <h2>Live Priority Queue & Incoming Cases</h2>
            <div style={ui.filters}>
              <input
                aria-label="Search patients"
                placeholder="Search patient name, case ID or complaint"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                style={{ flex: "2 1 240px" }}
              />
              <select
                aria-label="Filter priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                style={{ flex: "1 1 150px" }}
              >
                {priorityValues.map((p) => (
                  <option value={p} key={p}>
                    {p === "ALL" ? "All Priorities" : p}
                  </option>
                ))}
              </select>
            </div>
            <div
              style={{
                display: "grid",
                gridTemplateColumns:
                  "repeat(auto-fit, minmax(min(100%, 300px), 1fr))",
                gap: 22,
              }}
            >
              <div>
                <h3>Waiting in Hospital ({filteredWaiting.length})</h3>
                {filteredWaiting.length ? (
                  filteredWaiting.map((p) => (
                    <PatientRow key={p.id} patient={p} />
                  ))
                ) : (
                  <p>No matching waiting patients.</p>
                )}
              </div>
              <div>
                <h3>Pre-arrival ({filteredIncoming.length})</h3>
                {filteredIncoming.length ? (
                  filteredIncoming.map((p) => (
                    <PatientRow key={p.id} patient={p} incoming />
                  ))
                ) : (
                  <p>No matching incoming patients.</p>
                )}
              </div>
            </div>
          </section>}

          {mode !== "alerts" && <section style={ui.section}>
            <h2>
              Active Resource Allocations ({snapshot.activeAllocations.length})
            </h2>
            {snapshot.activeAllocations.length === 0 ? (
              <p>No patients currently assigned resources.</p>
            ) : (
              snapshot.activeAllocations.map((a) => (
                <div style={ui.row} key={a.id}>
                  <strong>
                    #{a.id} · {a.patientName} (Case #{a.caseId})
                  </strong>
                  <div style={{ ...ui.muted, marginTop: 5 }}>
                    Bed: {a.bedCode || "—"} · Doctor: {a.doctorName || "—"} ·
                    Equipment: {a.equipmentCode || "—"}
                  </div>
                </div>
              ))
            )}
            <h3 style={{ marginTop: 25 }}>Recent Allocation Activity</h3>
            {snapshot.recentActivity.length === 0 ? (
              <p>No allocation activity yet.</p>
            ) : (
              snapshot.recentActivity.map((a) => (
                <div style={ui.row} key={a.id}>
                  <strong>
                    Allocation #{a.id} — {a.status}
                  </strong>
                  <span style={ui.muted}>
                    {" "}
                    · {a.patientName} · {niceDate(a.createdAt)}
                  </span>
                </div>
              ))
            )}
          </section>}
        </>
      )}
    </div>
  );
}
