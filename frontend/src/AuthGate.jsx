import "./apiFetchSetup";
import { cloneElement, useCallback, useEffect, useState } from "react";

async function parseResponse(response) {
  const data = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new Error(
      data.message || data.detail || `Request failed (${response.status})`,
    );
  return data;
}

// Session cookie + server-issued CSRF token. Avoid storing auth tokens in localStorage.
async function postProtected(url, data = null, form = false) {
  const csrf = await parseResponse(
    await fetch("/api/auth/csrf", { credentials: "same-origin" }),
  );
  const headers = { [csrf.headerName]: csrf.token };
  if (data != null)
    headers["Content-Type"] = form
      ? "application/x-www-form-urlencoded"
      : "application/json";
  const body =
    data == null
      ? undefined
      : form
        ? new URLSearchParams(data).toString()
        : JSON.stringify(data);
  return parseResponse(
    await fetch(url, {
      method: "POST",
      credentials: "same-origin",
      headers,
      body,
    }),
  );
}

function RegistrationForm({ onRegistered }) {
  const [values, setValues] = useState({
    fullName: "",
    email: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const result = await postProtected("/api/auth/register", values);
      onRegistered(result.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <label>Full name</label>
      <input
        value={values.fullName}
        autoComplete="name"
        required
        onChange={(e) => setValues((v) => ({ ...v, fullName: e.target.value }))}
      />
      <label>Email</label>
      <input
        type="email"
        value={values.email}
        autoComplete="email"
        required
        onChange={(e) => setValues((v) => ({ ...v, email: e.target.value }))}
      />
      <label>Password (12+ characters)</label>
      <input
        type="password"
        minLength={12}
        maxLength={128}
        value={values.password}
        required
        autoComplete="new-password"
        onChange={(e) => setValues((v) => ({ ...v, password: e.target.value }))}
      />
      {error && (
        <p role="alert" style={{ color: "#b91c1c" }}>
          {error}
        </p>
      )}
      <button type="submit" disabled={busy}>
        {busy ? "Submitting..." : "Request staff account"}
      </button>
    </form>
  );
}

function AdminAccounts() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [working, setWorking] = useState(false);
  const reload = useCallback(async () => {
    try {
      setUsers(await parseResponse(await fetch("/api/admin/users")));
    } catch (err) {
      setError(err.message);
    }
  }, []);
  useEffect(() => {
    let active = true;
    fetch("/api/admin/users")
      .then(parseResponse)
      .then((data) => { if (active) setUsers(data); })
      .catch((err) => { if (active) setError(err.message); });
    return () => { active = false; };
  }, []);
  async function updateAccount(id, action) {
    setError("");
    setNotice("");
    setWorking(true);
    try {
      await postProtected(`/api/admin/users/${id}/${action}`);
      setNotice(`Account ${action} operation completed.`);
      await reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }
  return (
    <section className="card" style={{ marginBottom: 20 }}>
      <h2>Administrator — Staff Accounts</h2>
      <button type="button" onClick={reload}>
        Refresh Accounts
      </button>
      {error && (
        <p role="alert" style={{ color: "#b91c1c" }}>
          {error}
        </p>
      )}
      {notice && <p style={{ color: "#166534" }}>{notice}</p>}
      <div className="table-scroll" style={{ overflowX: "auto" }}>
        <table className="resource-table" style={{ width: "100%" }}>
          <thead>
            <tr>
              <th>Staff member</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.role}</td>
                <td>{u.status}</td>
                <td>
                  {u.role === "ADMIN" ? (
                    "Administrator"
                  ) : (
                    <>
                      {["PENDING", "DISABLED"].includes(u.status) && (
                        <button
                          type="button"
                          disabled={working}
                          onClick={() => updateAccount(u.id, "approve")}
                        >
                          Approve / Activate
                        </button>
                      )}
                      {u.status === "PENDING" && (
                        <button
                          type="button"
                          disabled={working}
                          onClick={() => updateAccount(u.id, "reject")}
                        >
                          Reject
                        </button>
                      )}
                      {u.status === "ACTIVE" && (
                        <button
                          type="button"
                          disabled={working}
                          onClick={() => updateAccount(u.id, "disable")}
                        >
                          Deactivate
                        </button>
                      )}
                      {u.status === "REJECTED" && "Rejected"}
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function AuthGate({ children }) {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [mode, setMode] = useState("login");
  const [login, setLogin] = useState({ email: "", password: "" });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    fetch("/api/auth/me", { credentials: "same-origin" })
      .then(async (res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (active) {
          setUser(data);
          setChecking(false);
        }
      })
      .catch(() => {
        if (active) {
          setUser(null);
          setChecking(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  async function signIn(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await postProtected("/api/auth/login", login, true);
      const me = await parseResponse(
        await fetch("/api/auth/me", { credentials: "same-origin" }),
      );
      setUser(me);
      setLogin({ email: "", password: "" });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }
  async function signOut() {
    setBusy(true);
    setError("");
    try {
      await postProtected("/api/auth/logout");
      setUser(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (checking) {
    return (
      <main className="auth-loading">
        <div className="auth-loading-mark">+</div>
        <h2>Preparing your workspace</h2>
        <p>Checking secure access...</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="auth-page">
        <section className="auth-story">
          <div className="auth-story-inner">
            <div className="auth-brand">
              <span className="auth-brand-icon">+</span>
              <span>Care<span className="auth-brand-light">Command</span></span>
            </div>
            <div className="auth-story-copy">
              <div className="auth-eyebrow"><span className="auth-eyebrow-dot"/> HOSPITAL EMERGENCY OPERATIONS</div>
              <h1>Better coordination.<br/><em>When it matters most.</em></h1>
              <p>A calmer way to manage urgent cases, coordinate care teams and keep critical resources in view.</p>
            </div>
            <div className="auth-preview" aria-hidden="true">
              <div className="auth-preview-top"><span><i/> Command center</span><small>LIVE WORKSPACE</small></div>
              <div className="auth-preview-metrics">
                <div><span>EMERGENCY CASES</span><strong>Priority-first</strong><small>Clear case visibility</small></div>
                <div><span>RESOURCE STATUS</span><strong>Always in view</strong><small>Beds · doctors · equipment</small></div>
              </div>
              <div className="auth-preview-bottom"><span className="auth-preview-circle">✦</span><span>One connected operational workflow</span><span className="auth-preview-bars"><b/><b/><b/></span></div>
            </div>
            <div className="auth-story-footer">OPENHACK'26 <span>·</span> TEAM 007 <span>·</span> HT-03</div>
          </div>
        </section>

        <section className="auth-form-side">
          <div className="auth-form-wrap">
            <div className="auth-form-top"><span className="auth-form-top-mark">✚</span> EMERGENCY RESOURCE MANAGER</div>
            <div className="auth-form-header">
              <div className="auth-small-heading">SECURE STAFF PORTAL</div>
              <h2>{mode === "login" ? "Welcome back" : "Request staff access"}</h2>
              <p>{mode === "login" ? "Sign in to continue to your hospital workspace." : "Create an account request for administrator approval."}</p>
            </div>
            {message && <div className="success" role="status">{message}</div>}
            {error && <div className="error" role="alert">{error}</div>}
            {mode === "login" ? (
              <form onSubmit={signIn} className="auth-form">
                <label>Email address
                  <input type="email" autoComplete="username" required placeholder="you@hospital.com" value={login.email} onChange={(e) => setLogin((v) => ({ ...v, email: e.target.value }))}/>
                </label>
                <label>Password
                  <input type="password" autoComplete="current-password" required placeholder="Enter your password" value={login.password} onChange={(e) => setLogin((v) => ({ ...v, password: e.target.value }))}/>
                </label>
                <button type="submit" disabled={busy} className="auth-submit">{busy ? "Signing in..." : "Sign in to workspace"} <span aria-hidden="true">→</span></button>
              </form>
            ) : (
              <RegistrationForm onRegistered={(text) => { setMessage(text); setMode("login"); }}/>
            )}
            <div className="auth-divider"><span>STAFF ACCESS</span></div>
            <button type="button" className="auth-mode-switch" onClick={() => { setMode((m) => m === "login" ? "register" : "login"); setError(""); setMessage(""); }}>
              {mode === "login" ? "New staff member? Request an account →" : "Already have an account? Sign in →"}
            </button>
            <div className="auth-note"><span>⌁</span> Access is limited to approved hospital staff. Use fictional patient data for demonstration.</div>
          </div>
        </section>
      </main>
    );
  }

  return cloneElement(children, {
    appUser: user,
    onSignOut: signOut,
    signOutBusy: busy,
    authError: error,
    adminPanel: user.role === "ADMIN" ? <AdminAccounts/> : null,
  });
}
