import AuthGate from "./AuthGate";
import { useEffect, useState } from "react";
import "./App.css";
import BedManagement from "./BedManagement";
import ResourceOperations from "./ResourceOperations";
import PatientCases from "./PatientCases";
import SmartAllocation from "./SmartAllocation";
import EmergencyDashboard from "./EmergencyDashboard";

const menu = [
  { id: "overview", label: "Command Center", icon: "dashboard", group: "WORKSPACE" },
  { id: "patients", label: "Emergency Patients", icon: "users", group: "WORKSPACE" },
  { id: "allocation", label: "Smart Allocation", icon: "layers", group: "WORKSPACE" },
  { id: "alerts", label: "Alerts & Shortages", icon: "bell", group: "WORKSPACE" },
  { id: "beds", label: "Beds & Rooms", icon: "bed", group: "MANAGEMENT" },
  { id: "resources", label: "Clinical Resources", icon: "heart", group: "MANAGEMENT" },
  { id: "hospitals", label: "Hospitals & Departments", icon: "building", group: "MANAGEMENT" },
  { id: "accounts", label: "Staff Accounts", icon: "shield", group: "ADMINISTRATION", admin: true },
];

function Icon({ name, size = 20 }) {
  const paths = {
    dashboard: <><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></>,
    users: <><circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2"/><path d="M17 5a3 3 0 0 1 0 6m2 9v-2a5 5 0 0 0-3-4.6"/></>,
    layers: <><path d="m12 2 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5M3 17l9 5 9-5"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Z"/><path d="M10 21h4"/></>,
    bed: <><path d="M3 5v15M3 17h18v3M3 13h18v4M6 13V8h5v5M11 10h7a3 3 0 0 1 3 3"/></>,
    heart: <><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/><path d="M4.5 12h4l2-3 3 6 2-3h4"/></>,
    building: <><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 21v-5h6v5M8 7h1m6 0h1M8 11h1m6 0h1"/></>,
    shield: <><path d="m12 2 9 4v6c0 5-3.5 8.5-9 10-5.5-1.5-9-5-9-10V6l9-4Z"/><path d="m9 12 2 2 4-4"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7"/></>,
    chevron: <path d="m7 10 5 5 5-5"/>,
    menu: <path d="M4 6h16M4 12h16M4 18h16"/>,
    close: <path d="M5 5l14 14M19 5 5 19"/>,
    activity: <path d="M3 12h4l3-7 4 14 3-7h4"/>,
  };
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.dashboard}</svg>;
}

const headings = {
  overview: ["Emergency Command Center", "Your hospital at a glance, updated in real time."],
  patients: ["Emergency Patients", "Register cases, manage arrivals and prioritize care."],
  allocation: ["Smart-Assisted Allocation", "Assign the right resources with staff oversight."],
  alerts: ["Alerts & Shortages", "Monitor critical cases and resource availability."],
  beds: ["Beds & Rooms", "Manage locations and track live bed availability."],
  resources: ["Clinical Resources", "Doctors, ambulances and medical equipment."],
  hospitals: ["Hospitals & Departments", "Manage your facility and its departments."],
  accounts: ["Staff Accounts", "Review and manage access requests."],
};

function App({ appUser, onSignOut, signOutBusy, adminPanel, authError }) {
  const [active, setActive] = useState("overview");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [hospitals, setHospitals] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [departments, setDepartments] = useState([]);
  const [hospitalForm, setHospitalForm] = useState({ name: "", city: "", address: "" });
  const [departmentName, setDepartmentName] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadHospitals() {
    const response = await fetch("/api/hospitals");
    if (!response.ok) throw new Error("Failed to load hospitals");
    const data = await response.json();
    setHospitals(data);
    setSelectedId((current) => data.some((h) => String(h.id) === current)
      ? current : (data.length ? String(data[0].id) : ""));
  }

  async function loadDepartments(hospitalId) {
    const response = await fetch(`/api/hospitals/${hospitalId}/departments`);
    if (!response.ok) throw new Error("Failed to load departments");
    setDepartments(await response.json());
  }

  useEffect(() => {
    let activeRequest = true;
    fetch("/api/hospitals")
      .then((res) => { if (!res.ok) throw new Error("Failed to load hospitals"); return res.json(); })
      .then((data) => {
        if (!activeRequest) return;
        setHospitals(data);
        setSelectedId((current) => data.some((h) => String(h.id) === current)
          ? current : (data.length ? String(data[0].id) : ""));
      })
      .catch((err) => { if (activeRequest) setError(err.message); });
    return () => { activeRequest = false; };
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    let activeRequest = true;
    fetch(`/api/hospitals/${selectedId}/departments`)
      .then((res) => { if (!res.ok) throw new Error("Failed to load departments"); return res.json(); })
      .then((data) => { if (activeRequest) setDepartments(data); })
      .catch((err) => { if (activeRequest) setError(err.message); });
    return () => { activeRequest = false; };
  }, [selectedId]);

  async function addHospital(event) {
    event.preventDefault();
    setError(""); setMessage("");
    try {
      const response = await fetch("/api/hospitals", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(hospitalForm),
      });
      if (!response.ok) throw new Error("Could not register hospital");
      const created = await response.json();
      await loadHospitals();
      setSelectedId(String(created.id));
      setHospitalForm({ name: "", city: "", address: "" });
      setMessage("Hospital registered successfully");
    } catch (err) { setError(err.message); }
  }

  async function addDepartment(event) {
    event.preventDefault();
    setError(""); setMessage("");
    try {
      const response = await fetch(`/api/hospitals/${selectedId}/departments`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: departmentName }),
      });
      if (!response.ok) throw new Error("Could not add department. Check for duplicates.");
      setDepartmentName("");
      await loadDepartments(selectedId);
      setMessage("Department added successfully");
    } catch (err) { setError(err.message); }
  }

  const selectedHospital = hospitals.find((h) => String(h.id) === selectedId);
  const visibleMenu = menu.filter((item) => !item.admin || appUser?.role === "ADMIN");
  const [pageTitle, pageSubtitle] = headings[active];

  function navigate(id) {
    setActive(id);
    setMobileOpen(false);
    setError(""); setMessage("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  return (
    <div className="app-shell">
      {mobileOpen && <button className="sidebar-scrim" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
      <aside className={`app-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <div className="brand-block">
          <div className="brand-symbol"><Icon name="activity" size={27}/></div>
          <div><div className="brand-name">Care<span>Command</span></div><div className="brand-tag">EMERGENCY OPERATIONS</div></div>
          <button className="mobile-close icon-button" aria-label="Close navigation" onClick={() => setMobileOpen(false)}><Icon name="close"/></button>
        </div>
        <div className="sidebar-hospital">
          <span className="sidebar-hospital-label">ACTIVE FACILITY</span>
          <div className="sidebar-hospital-name">{selectedHospital?.name || "No hospital selected"}</div>
          <div className="sidebar-hospital-city">{selectedHospital?.city || "Select a facility above"}</div>
        </div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          {["WORKSPACE", "MANAGEMENT", "ADMINISTRATION"].map((group) => {
            const items = visibleMenu.filter((item) => item.group === group);
            return items.length ? (
              <div className="nav-group" key={group}>
                <p className="nav-group-label">{group}</p>
                {items.map((item) => (
                  <button type="button" key={item.id} className={`nav-link ${active === item.id ? "active" : ""}`} onClick={() => navigate(item.id)} aria-current={active === item.id ? "page" : undefined}>
                    <Icon name={item.icon} size={19}/><span>{item.label}</span>
                    {active === item.id && <span className="nav-active-dot"/>}
                  </button>
                ))}
              </div>
            ) : null;
          })}
        </nav>
        <div className="sidebar-footer">
          <div className="sidebar-footer-line"><span className="online-dot"/> Operational workspace</div>
          <div>Team 007 · OpenHack'26</div>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-left">
            <button type="button" className="icon-button menu-toggle" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Icon name="menu"/></button>
            <span className="topbar-breadcrumb">Workspace</span><span className="breadcrumb-slash">/</span><strong>{pageTitle}</strong>
          </div>
          <div className="topbar-right">
            <label className="topbar-hospital-select">
              <span>Facility</span>
              <select aria-label="Select hospital" value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
                {hospitals.length === 0 && <option value="">No hospitals</option>}
                {hospitals.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
              </select>
            </label>
            <div className="user-avatar" aria-hidden="true">{(appUser?.fullName || "U").split(" ").slice(0, 2).map((w) => w[0]).join("").toUpperCase()}</div>
            <div className="topbar-user"><strong>{appUser?.fullName || "Staff"}</strong><span>{appUser?.role === "ADMIN" ? "Administrator" : "Hospital Staff"}</span></div>
            <button type="button" className="signout-button" onClick={onSignOut} disabled={signOutBusy} title="Sign out"><Icon name="logout" size={18}/><span>Sign out</span></button>
          </div>
        </header>

        <main className="content-area">
          <div className="page-heading">
            <div><div className="page-eyebrow"><span className="eyebrow-dot"/> LIVE OPERATIONS <span className="eyebrow-divider">/</span> {active.toUpperCase()}</div>
              <h1>{pageTitle}</h1><p>{pageSubtitle}</p>
            </div>
            <div className="heading-date">{new Date().toLocaleDateString(undefined, {weekday:"short", month:"short", day:"numeric", year:"numeric"})}</div>
          </div>

          {authError && <div className="error" role="alert">{authError}</div>}
          {message && <div className="success" role="status">{message}</div>}
          {error && <div className="error" role="alert">{error}</div>}

          {active === "overview" && <EmergencyDashboard hospitalId={selectedId}/>}
          {active === "alerts" && <EmergencyDashboard hospitalId={selectedId} mode="alerts"/>}
          {active === "patients" && <PatientCases hospitalId={selectedId}/>}
          {active === "allocation" && <SmartAllocation hospitalId={selectedId}/>}
          {active === "beds" && <BedManagement hospitalId={selectedId}/>}
          {active === "resources" && <ResourceOperations hospitalId={selectedId}/>}
          {active === "accounts" && appUser?.role === "ADMIN" && adminPanel}

          {active === "hospitals" && (
            <div className="setup-grid">
              {appUser?.role === "ADMIN" && <section className="card">
                <div className="card-kicker">FACILITY SETUP</div><h2>Register Hospital</h2>
                <p className="card-description">Add a facility to your emergency network.</p>
                <form onSubmit={addHospital}>
                  <label>Hospital name<input placeholder="e.g. City General Hospital" value={hospitalForm.name} onChange={(e) => setHospitalForm({...hospitalForm, name:e.target.value})} required/></label>
                  <label>City<input placeholder="City" value={hospitalForm.city} onChange={(e) => setHospitalForm({...hospitalForm, city:e.target.value})} required/></label>
                  <label>Address<input placeholder="Street address" value={hospitalForm.address} onChange={(e) => setHospitalForm({...hospitalForm, address:e.target.value})} required/></label>
                  <button type="submit">Register Hospital</button>
                </form>
              </section>}
              <section className="card">
                <div className="card-kicker">ORGANIZATION</div><h2>Manage Departments</h2>
                <p className="card-description">Create and review departments at the selected hospital.</p>
                {hospitals.length === 0 ? <p>Register a hospital first.</p> : <>
                  <label>Selected hospital<select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
                    {hospitals.map((h) => <option key={h.id} value={h.id}>{h.name} — {h.city}</option>)}
                  </select></label>
                  <form onSubmit={addDepartment}>
                    <label>New department<input placeholder="e.g. Emergency" value={departmentName} onChange={(e) => setDepartmentName(e.target.value)} required/></label>
                    <button type="submit">Add Department</button>
                  </form>
                  <h3>Registered Departments</h3>
                  {departments.length ? <div className="department-list">{departments.map((d) => <div className="department" key={d.id}>{d.name}</div>)}</div> : <p>No departments registered.</p>}
                </>}
              </section>
            </div>
          )}
          <footer className="content-footer">CareCommand <span>·</span> Emergency Resource Manager <span>·</span> Prototype — use fictional patient data only</footer>
        </main>
      </div>
    </div>
  );
}

export default function SecuredApp() {
  return <AuthGate><App/></AuthGate>;
}
