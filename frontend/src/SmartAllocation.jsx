import { useEffect, useState } from "react";

// Session 6 revised. Replaces the OLD SmartAllocation.jsx component.
// No temporary holds or auto-scoring; staff choose eligible resources.
const priority = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
const formatTime = (value) => (value ? new Date(value).toLocaleString() : "—");

export default function SmartAllocation({ hospitalId }) {
  const [cases, setCases] = useState([]);
  const [allocations, setAllocations] = useState([]);
  const [caseId, setCaseId] = useState("");
  const [catalog, setCatalog] = useState(null);
  const [chosen, setChosen] = useState({
    bedId: "",
    doctorId: "",
    equipmentId: "",
  });
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const base = `/api/hospitals/${hospitalId}`;

  async function api(path, options = {}) {
    const response = await fetch(path, options);
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(
        data.detail ||
          data.message ||
          `Request failed (HTTP ${response.status})`,
      );
    }
    return data;
  }

  async function loadLists() {
    if (!hospitalId) return;
    const [caseData, allocationData] = await Promise.all([
      api(`${base}/cases`),
      api(`${base}/allocations`),
    ]);
    setCases(caseData);
    setAllocations(allocationData);
  }

  async function loadCatalog(id) {
    if (!id) {
      setCatalog(null);
      return;
    }
    const result = await api(`${base}/allocations/cases/${id}/resources`);
    setCatalog(result);
  }

  useEffect(() => {
    setCases([]);
    setAllocations([]);
    setCaseId("");
    setCatalog(null);
    setChosen({ bedId: "", doctorId: "", equipmentId: "" });
    setMessage("");
    setError("");
    if (!hospitalId) return;

    let mounted = true;
    Promise.all([api(`${base}/cases`), api(`${base}/allocations`)])
      .then(([caseData, allocationData]) => {
        if (!mounted) return;
        setCases(caseData);
        setAllocations(allocationData);
      })
      .catch((e) => {
        if (mounted) setError(e.message);
      });
    return () => {
      mounted = false;
    };
  }, [hospitalId]);

  async function changeCase(id) {
    setCaseId(id);
    setCatalog(null);
    setChosen({ bedId: "", doctorId: "", equipmentId: "" });
    setError("");
    setMessage("");
    if (!id) return;
    try {
      await loadCatalog(id);
    } catch (e) {
      setError(e.message);
    }
  }

  async function refresh() {
    setBusy(true);
    setError("");
    try {
      await loadLists();
      if (caseId) await loadCatalog(caseId);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  const selectedBed = catalog?.beds.find((b) => String(b.id) === chosen.bedId);

  function allowedEquipment(e) {
    return (
      e.eligible && !!selectedBed && e.departmentId === selectedBed.departmentId
    );
  }

  const selectedEquipment = catalog?.equipment.find(
    (e) => String(e.id) === chosen.equipmentId,
  );
  const allSelected =
    !!catalog &&
    catalog.canConfirm &&
    (catalog.needsBed || catalog.needsDoctor || catalog.needsVentilator) &&
    (!catalog.needsBed || chosen.bedId) &&
    (!catalog.needsDoctor || chosen.doctorId) &&
    (!catalog.needsVentilator ||
      (chosen.equipmentId &&
        selectedEquipment &&
        allowedEquipment(selectedEquipment)));

  async function confirm() {
    if (!allSelected || busy) return;
    if (
      !window.confirm("Confirm staff-selected resources and START treatment?")
    )
      return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await api(`${base}/allocations/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          caseId: Number(caseId),
          bedId: chosen.bedId ? Number(chosen.bedId) : null,
          doctorId: chosen.doctorId ? Number(chosen.doctorId) : null,
          equipmentId: chosen.equipmentId ? Number(chosen.equipmentId) : null,
        }),
      });
      setMessage(
        "Allocation confirmed. Patient is UNDER_TREATMENT and resources are assigned.",
      );
      setCaseId("");
      setCatalog(null);
      setChosen({ bedId: "", doctorId: "", equipmentId: "" });
      await loadLists();
    } catch (e) {
      setError(e.message);
      await loadLists().catch(() => {});
      if (caseId) await loadCatalog(caseId).catch(() => {});
    } finally {
      setBusy(false);
    }
  }

  async function allocationAction(id, action) {
    const description =
      action === "complete"
        ? "Confirm treatment is complete and release ALL assigned resources?"
        : "Cancel this legacy temporary hold?";
    if (!window.confirm(description)) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await api(`${base}/allocations/${id}/${action}`, { method: "POST" });
      await loadLists();
      if (caseId) await loadCatalog(caseId).catch(() => {});
      setMessage(
        action === "complete"
          ? "Treatment completed and assigned resources released."
          : "Legacy temporary hold cancelled.",
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  const waitingCases = [...cases]
    .filter((c) => c.caseStatus === "WAITING")
    .sort(
      (a, b) =>
        (priority[a.urgency] ?? 99) - (priority[b.urgency] ?? 99) ||
        Date.parse(a.arrivedAt || a.registeredAt) -
          Date.parse(b.arrivedAt || b.registeredAt),
    );

  if (!hospitalId)
    return (
      <section className="card">
        <h2>Manual Allocation</h2>
        <p>Select a hospital first.</p>
      </section>
    );

  function ResourceSelect({ title, type, list }) {
    const selected = chosen[type];
    return (
      <div style={{ marginTop: 16 }}>
        <label>
          <strong>{title}</strong>
        </label>
        <select
          value={selected}
          disabled={busy}
          onChange={(e) =>
            setChosen((old) => ({
              ...old,
              [type]: e.target.value,
              ...(type === "bedId" ? { equipmentId: "" } : {}),
            }))
          }
        >
          <option value="">Select an eligible resource</option>
          {list
            .filter((x) =>
              type !== "equipmentId" ? x.eligible : allowedEquipment(x),
            )
            .map((x) => (
              <option value={x.id} key={x.id}>
                {x.label}
              </option>
            ))}
        </select>
        {list.filter((x) =>
          type !== "equipmentId" ? x.eligible : allowedEquipment(x),
        ).length === 0 && (
          <p style={{ color: "#9a3412" }}>
            No eligible resources found. Check the review table below.
          </p>
        )}
      </div>
    );
  }

  function ResourceList({ title, items, equipmentList = false }) {
    return (
      <div style={{ marginTop: 20 }}>
        <h3>{title}</h3>
        {items.length === 0 ? (
          <p>No resources registered in this hospital.</p>
        ) : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead>
                <tr>
                  <th>Resource</th>
                  <th>Eligibility</th>
                  <th>Explanation</th>
                </tr>
              </thead>
              <tbody>
                {items.map((x) => {
                  const sameDept =
                    !equipmentList ||
                    !selectedBed ||
                    x.departmentId === selectedBed.departmentId;
                  const available = x.eligible && sameDept;
                  const explanations = [...x.reasons];
                  if (equipmentList && selectedBed && !sameDept)
                    explanations.push(
                      "Different department from the selected bed; transfer required before use",
                    );
                  return (
                    <tr key={x.id}>
                      <td>{x.label}</td>
                      <td>
                        <strong
                          style={{ color: available ? "#166534" : "#9a3412" }}
                        >
                          {available ? "ELIGIBLE" : "REQUIRES REVIEW"}
                        </strong>
                      </td>
                      <td>
                        {explanations.length === 0
                          ? "Available and compatible"
                          : explanations.join("; ")}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="resource-management">
      <h2>Smart-Assisted Manual Allocation</h2>
      <p>
        Choose resources yourself. The system displays EVERY registered
        resource, explains unavailable/incompatible ones, and rechecks
        everything before confirming.
      </p>
      <p>
        <strong>Demo only:</strong> Priority and requirements must come from
        qualified clinical staff. This is a demonstration system, not clinically
        validated.
      </p>
      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}

      <section className="card">
        <h2>1. Select Emergency Patient</h2>
        <select
          disabled={busy}
          value={caseId}
          onChange={(e) => changeCase(e.target.value)}
        >
          <option value="">Select a waiting case</option>
          {waitingCases.map((c) => (
            <option value={c.id} key={c.id}>
              #{c.id} — {c.patientName} — {c.urgency} — {c.arrivalStatus}
            </option>
          ))}
        </select>
        <button
          type="button"
          disabled={busy}
          style={{ marginTop: 12 }}
          onClick={refresh}
        >
          Refresh Cases & Resources
        </button>

        {catalog && (
          <>
            <h3>
              Case #{catalog.caseId}: {catalog.patientName}
            </h3>
            <p>
              <strong>Priority:</strong> {catalog.urgency} |{" "}
              <strong>Arrival:</strong> {catalog.arrivalStatus}
            </p>
            <p>
              <strong>Required bed:</strong>{" "}
              {catalog.requestedBedType ||
                (catalog.needsBed ? "Any compatible bed" : "None")}
              {" | "}
              <strong>Specialization:</strong>{" "}
              {catalog.requestedSpecialization || "Not specified"}
            </p>
            <p>
              <strong>Oxygen:</strong> {catalog.oxygenNeeded ? "Yes" : "No"}
              {" | "}
              <strong>Ventilator:</strong>{" "}
              {catalog.ventilatorNeeded ? "Yes" : "No"}
            </p>

            {!catalog.canConfirm && (
              <div className="error">
                This patient is PRE_ARRIVAL. Confirm arrival in Patient
                Registration before allocating resources.
              </div>
            )}
            {!catalog.needsBed &&
              !catalog.needsDoctor &&
              !catalog.needsVentilator && (
                <div className="error">
                  No resource requirements were entered for this case. Register
                  a case with requirements to test allocation.
                </div>
              )}

            {catalog.needsBed && (
              <ResourceSelect
                title="Choose Bed"
                type="bedId"
                list={catalog.beds}
              />
            )}
            {catalog.needsDoctor && (
              <ResourceSelect
                title="Choose Doctor"
                type="doctorId"
                list={catalog.doctors}
              />
            )}
            {catalog.needsVentilator && (
              <>
                {!chosen.bedId && (
                  <p>
                    Select a bed first; a ventilator must belong to that bed's
                    department.
                  </p>
                )}
                <ResourceSelect
                  title="Choose Ventilator"
                  type="equipmentId"
                  list={catalog.equipment}
                />
              </>
            )}

            <button
              type="button"
              disabled={busy || !allSelected}
              style={{ marginTop: 20 }}
              onClick={confirm}
            >
              Confirm Allocation & Start Treatment
            </button>
            <p>
              <small>
                Resources are not held in advance. If another staff member
                claims a resource first, the server rejects the conflicting
                request.
              </small>
            </p>

            {catalog.needsBed && (
              <ResourceList
                title="All Hospital Beds — Availability & Reasons"
                items={catalog.beds}
              />
            )}
            {catalog.needsDoctor && (
              <ResourceList
                title="All Doctors — Availability & Reasons"
                items={catalog.doctors}
              />
            )}
            {catalog.needsVentilator && (
              <ResourceList
                title="All Equipment — Availability & Reasons"
                items={catalog.equipment}
                equipmentList
              />
            )}
          </>
        )}
      </section>

      <section className="card">
        <h2>2. Confirmed Allocations & History</h2>
        <p>
          Approved allocations can be completed, returning all their assigned
          resources to the available pool.
        </p>
        {allocations.length === 0 ? (
          <p>No allocation records yet.</p>
        ) : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead>
                <tr>
                  <th>ID / Patient</th>
                  <th>Bed</th>
                  <th>Doctor</th>
                  <th>Equipment</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {allocations.map((a) => (
                  <tr key={a.id}>
                    <td>
                      #{a.id}: {a.patientName}
                      <div>
                        <small>Case: {a.caseStatus}</small>
                      </div>
                    </td>
                    <td>{a.bedCode || "—"}</td>
                    <td>{a.doctorName || "—"}</td>
                    <td>{a.equipmentCode || "—"}</td>
                    <td>
                      <strong>{a.status}</strong>
                      <div>
                        <small>{formatTime(a.approvedAt || a.createdAt)}</small>
                      </div>
                    </td>
                    <td>
                      {a.status === "APPROVED" ? (
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => allocationAction(a.id, "complete")}
                        >
                          Complete & Release
                        </button>
                      ) : a.status === "HELD" ? (
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => allocationAction(a.id, "cancel")}
                        >
                          Cancel Old Hold
                        </button>
                      ) : (
                        "Closed"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <p>
        Ambulance dispatch remains in Ambulance Management. Dashboard
        synchronization and staff authentication are upcoming work.
      </p>
    </div>
  );
}
