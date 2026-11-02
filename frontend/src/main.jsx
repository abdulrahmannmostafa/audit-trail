import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";
import {
  getAccessToken,
  getRedirectUri,
  getUserProfile,
  initializeAuth,
  login as keycloakLogin,
  logout as keycloakLogout,
} from "./auth";

const api = async (path, options = {}) => {
  const token = await getAccessToken();
  const headers = {
    ...(options.body instanceof FormData
      ? {}
      : { "Content-Type": "application/json" }),
    ...(options.headers || {}),
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(
      body.message || body.error || `Request failed (${response.status})`,
    );
  }
  if (response.status === 204) return null;
  return response.json();
};

const fileApi = async (path) => {
  const token = await getAccessToken();
  const response = await fetch(path, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) throw new Error(`Download failed (${response.status})`);
  return {
    blob: await response.blob(),
    disposition: response.headers.get("content-disposition"),
  };
};

const icons = {
  grid: "▦",
  files: "▤",
  activity: "◌",
  mail: "✉",
  users: "♙",
  search: "⌕",
  plus: "+",
  arrow: "→",
  download: "↓",
  logout: "↪",
  shield: "◆",
  check: "✓",
  close: "×",
  refresh: "↻",
  trash: "⌫",
};
const navItems = [
  ["Overview", icons.grid],
  ["Documents", icons.files],
  ["Audit trail", icons.activity],
  ["Email center", icons.mail],
  ["Identity & roles", icons.users],
];
const today = () => new Date().toISOString().slice(0, 10);
const formatDate = (value) =>
  value
    ? new Intl.DateTimeFormat("en", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";
const formatSize = (value) =>
  value == null ? "—" : `${(Number(value) / 1024 / 1024).toFixed(2)} MB`;
const errorText = (error) =>
  error instanceof Error ? error.message : "Something went wrong";
const templateVariables = (template) => {
  const source = `${template?.subjectTemplate || ""} ${template?.htmlContent || ""}`;
  return [
    ...new Set(
      [...source.matchAll(/\{\{\s*([\w.-]+)\s*\}\}/g)].map((match) => match[1]),
    ),
  ];
};
const variableLabel = (name) =>
  name
    .split(/[.-]/)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");

function App() {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [authError, setAuthError] = useState("");
  const [page, setPage] = useState("Overview");
  const [toast, setToast] = useState("");
  const [globalSearch, setGlobalSearch] = useState("");
  const [assistantOpen, setAssistantOpen] = useState(false);
  useEffect(() => {
    initializeAuth()
      .then(setAuthenticated)
      .catch((error) => {
        setAuthError(errorText(error));
        setAuthenticated(false);
      })
      .finally(() => setReady(true));
  }, []);
  const notify = (message) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 3500);
  };
  if (!ready) return <Loading title="Connecting to Keycloak" />;
  if (authError)
    return (
      <AuthError message={authError} retry={() => window.location.reload()} />
    );
  if (!authenticated) return <Login onLogin={keycloakLogin} />;
  return (
    <div className="app-shell">
      <Sidebar page={page} setPage={setPage} logout={keycloakLogout} />
      <main className="main">
        <Topbar page={page} search={globalSearch} setSearch={setGlobalSearch} />
        <div className="content">
          <Page
            page={page}
            setPage={setPage}
            notify={notify}
            globalSearch={globalSearch}
          />
        </div>
      </main>
      {toast && (
        <div className="toast">
          <span>{icons.check}</span>
          {toast}
        </div>
      )}
      <Assistant
        open={assistantOpen}
        setOpen={setAssistantOpen}
        page={page}
        setPage={setPage}
      />
    </div>
  );
}

function Loading({ title }) {
  return (
    <main className="login-page">
      <div className="login-card loading-login">
        <span className="loader" />
        <h2>{title}</h2>
        <p className="muted">Preparing your secure workspace...</p>
      </div>
    </main>
  );
}
function Login({ onLogin }) {
  return (
    <main className="login-page">
      <div className="login-art">
        <div className="orb orb-one" />
        <div className="orb orb-two" />
        <span className="brand-mark">{icons.shield}</span>
        <p className="eyebrow">QEEMA UTIL</p>
        <h1>
          Your work,
          <br />
          <em>in control.</em>
        </h1>
        <p className="muted">
          A secure command center for documents, people, communications and
          every important action.
        </p>
      </div>
      <div className="login-card">
        <Brand />
        <div className="login-copy">
          <p className="eyebrow">WELCOME BACK</p>
          <h2>Sign in to your workspace</h2>
          <p className="muted">
            Use your secure Keycloak account to access the workspace.
          </p>
        </div>
        <button className="primary full" onClick={onLogin}>
          Continue with Keycloak {icons.arrow}
        </button>
        <p className="hint">
          Authentication is handled securely by your organization identity
          provider.
        </p>
      </div>
    </main>
  );
}
function AuthError({ message, retry }) {
  return (
    <main className="login-page">
      <div className="login-card loading-login">
        <span className="error-symbol">{icons.close}</span>
        <p className="eyebrow">AUTHENTICATION UNAVAILABLE</p>
        <h2>Could not connect to Keycloak</h2>
        <p className="muted">{message}</p>
        <button className="primary full" onClick={retry}>
          Try again
        </button>
        <p className="hint">
          Register this exact redirect URI: <code>{getRedirectUri()}</code>
        </p>
      </div>
    </main>
  );
}
function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark small">{icons.shield}</span>
      <span>
        Qeema <b>Workspace</b>
      </span>
    </div>
  );
}
function Sidebar({ page, setPage, logout }) {
  const profile = getUserProfile();
  return (
    <aside className="sidebar">
      <Brand />
      <div className="workspace-switch">
        <span className="status-dot" /> Production workspace <span>⌄</span>
      </div>
      <p className="nav-label">WORKSPACE</p>
      <nav>
        {navItems.map(([name, icon]) => (
          <button
            className={page === name ? "nav-item active" : "nav-item"}
            onClick={() => setPage(name)}
            key={name}
          >
            <span>{icon}</span>
            {name}
            {name === "Audit trail" && <i>Live</i>}
          </button>
        ))}
      </nav>
      <div className="sidebar-bottom">
        <div className="profile">
          <div className="avatar">{(profile.name || "A")[0]}</div>
          <div>
            <strong>{profile.name}</strong>
            <small>{profile.username}</small>
          </div>
          <button onClick={logout} title="Log out">
            {icons.logout}
          </button>
        </div>
      </div>
    </aside>
  );
}
function Topbar({ page, search, setSearch }) {
  return (
    <header className="topbar">
      <div>
        <p className="breadcrumb">
          Workspace <span>/</span> {page}
        </p>
        <h1>{page}</h1>
      </div>
      <div className="top-actions">
        <div className="global-search">
          {icons.search}
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search this view..."
          />
        </div>
        <div className="avatar">{(getUserProfile().name || "A")[0]}</div>
      </div>
    </header>
  );
}
function Page({ page, setPage, notify, globalSearch }) {
  if (page === "Documents")
    return <Documents notify={notify} search={globalSearch} />;
  if (page === "Audit trail") return <Audit search={globalSearch} />;
  if (page === "Email center")
    return <Email notify={notify} search={globalSearch} />;
  if (page === "Identity & roles") return <Identity notify={notify} />;
  return <Overview setPage={setPage} notify={notify} />;
}

function Overview({ setPage, notify }) {
  const [state, setState] = useState({
    user: null,
    docs: [],
    audit: null,
    templates: [],
    roles: [],
  });
  const [loading, setLoading] = useState(true);
  const load = () =>
    Promise.allSettled([
      api("/api/me"),
      api("/api/documents"),
      api("/api/admin/audit-logs?size=6"),
      api("/api/admin/email-templates"),
      api("/api/admin/roles"),
    ])
      .then((results) => {
        const value = (index) =>
          results[index].status === "fulfilled" ? results[index].value : null;
        setState({
          user: value(0),
          docs: value(1) || [],
          audit: value(2),
          templates: value(3) || [],
          roles: value(4) || [],
        });
      })
      .finally(() => setLoading(false));
  useEffect(() => {
    load();
  }, []);
  const events = state.audit?.content || [];
  return (
    <>
      <section className="hero">
        <div>
          <p className="eyebrow">
            {new Intl.DateTimeFormat("en", { dateStyle: "full" })
              .format(new Date())
              .toUpperCase()}
          </p>
          <h2>
            Good morning
            {state.user?.displayName
              ? `, ${state.user.displayName.split(" ")[0]}`
              : ""}{" "}
            <span>✦</span>
          </h2>
          <p className="muted">
            Everything important in your workspace, live from the API.
          </p>
        </div>
        <button className="primary" onClick={() => setPage("Documents")}>
          Open documents {icons.arrow}
        </button>
      </section>
      {loading ? (
        <Loading title="Loading workspace" />
      ) : (
        <>
          <div className="stat-grid">
            <Stat
              label="Accessible documents"
              value={state.docs.length}
              trend="Current API result"
              icon={icons.files}
            />
            <Stat
              label="Audit events loaded"
              value={state.audit?.totalElements ?? 0}
              trend="Protected history"
              icon={icons.activity}
            />
            <Stat
              label="Email templates"
              value={state.templates.length}
              trend="Configured templates"
              icon={icons.mail}
            />
            <Stat
              label="Available roles"
              value={state.roles.length}
              trend="Admin catalog"
              icon={icons.shield}
            />
          </div>
          <div className="overview-grid">
            <section className="panel activity-panel">
              <PanelTitle
                title="Recent activity"
                action="View audit trail"
                onAction={() => setPage("Audit trail")}
              />
              {events.length ? (
                <div className="timeline">
                  {events.map((event) => (
                    <div className="timeline-row" key={event.eventId}>
                      <span className="event-dot" />
                      <div>
                        <strong>{event.action}</strong>
                        <small>
                          {event.entityType} {event.entityId || ""} ·{" "}
                          {formatDate(event.occurredAt)}
                        </small>
                      </div>
                      <span
                        className={`outcome ${event.outcome?.toLowerCase()}`}
                      >
                        {event.outcome}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty
                  title="No activity yet"
                  text="Protected events will appear here after your first operation."
                />
              )}
            </section>
            <section className="panel quick-panel">
              <PanelTitle title="Quick actions" />
              <Quick
                label="Upload a document"
                icon={icons.files}
                onClick={() => setPage("Documents")}
              />
              <Quick
                label="Send an email"
                icon={icons.mail}
                onClick={() => setPage("Email center")}
              />
              <Quick
                label="Review audit trail"
                icon={icons.activity}
                onClick={() => setPage("Audit trail")}
              />
            </section>
          </div>
        </>
      )}
    </>
  );
}
function Stat({ label, value, trend, icon }) {
  return (
    <div className="stat-card">
      <div className="stat-top">
        <span className="stat-icon">{icon}</span>
        <span className="pill green">{icons.check} Live</span>
      </div>
      <strong>{value}</strong>
      <span>{label}</span>
      <small>{trend}</small>
    </div>
  );
}
function PanelTitle({ title, action, onAction }) {
  return (
    <div className="panel-title">
      <h3>{title}</h3>
      {action && (
        <button className="text-button" onClick={onAction}>
          {action} {icons.arrow}
        </button>
      )}
    </div>
  );
}
function Quick({ label, icon, onClick }) {
  return (
    <button className="quick-action" onClick={onClick}>
      <span>{icon}</span>
      <strong>{label}</strong>
      <b>{icons.arrow}</b>
    </button>
  );
}
function Empty({ title, text }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">{icons.activity}</span>
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}
function PageIntro({ eyebrow, title, text, action }) {
  return (
    <section className="page-intro">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        <p className="muted">{text}</p>
      </div>
      {action}
    </section>
  );
}
function ErrorBox({ message, onRetry }) {
  return (
    <div className="error-box">
      <strong>{message}</strong>
      {onRetry && (
        <button className="secondary" onClick={onRetry}>
          {icons.refresh} Retry
        </button>
      )}
    </div>
  );
}

function Documents({ notify, search }) {
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [versions, setVersions] = useState(null);
  const load = () => {
    setLoading(true);
    api("/api/documents")
      .then(setDocs)
      .catch((error) => setError(errorText(error)))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    load();
  }, []);
  const visible = docs.filter((doc) =>
    `${doc.title} ${doc.filename} ${doc.description || ""}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  );
  const download = async (doc) => {
    try {
      const result = await fileApi(`/api/documents/${doc.id}/download`);
      const url = URL.createObjectURL(result.blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = doc.filename || doc.title;
      link.click();
      URL.revokeObjectURL(url);
      notify("Download started");
    } catch (error) {
      notify(errorText(error));
    }
  };
  const remove = async (doc) => {
    if (!window.confirm(`Delete "${doc.title}"?`)) return;
    try {
      await api(`/api/documents/${doc.id}`, { method: "DELETE" });
      notify("Document deleted");
      load();
    } catch (error) {
      notify(errorText(error));
    }
  };
  return (
    <>
      <PageIntro
        eyebrow="DOCUMENT MANAGEMENT"
        title="Your documents"
        text="Upload, version, download and retire files through the protected document API."
        action={
          <button className="primary" onClick={() => setUploadOpen(true)}>
            {icons.plus} Upload document
          </button>
        }
      />
      {error && <ErrorBox message={error} onRetry={load} />}
      {loading ? (
        <Loading title="Loading documents" />
      ) : (
        <section className="panel table-panel">
          <PanelTitle title={`${visible.length} accessible documents`} />
          <table>
            <thead>
              <tr>
                <th>DOCUMENT</th>
                <th>VERSION</th>
                <th>SIZE</th>
                <th>UPDATED</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((doc) => (
                <tr key={doc.id}>
                  <td>
                    <div className="file-cell">
                      <span className="file-icon">{icons.files}</span>
                      <div>
                        <strong>{doc.title}</strong>
                        <small>
                          {doc.filename} · {doc.contentType}
                        </small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className="version">Current</span>
                  </td>
                  <td>{formatSize(doc.sizeBytes)}</td>
                  <td>{formatDate(doc.updatedAt)}</td>
                  <td className="row-actions">
                    <button
                      title="Versions"
                      onClick={() =>
                        api(`/api/documents/${doc.id}/versions`)
                          .then((data) => setVersions({ doc, data }))
                          .catch((error) => notify(errorText(error)))
                      }
                    >
                      {icons.activity}
                    </button>
                    <button title="Download" onClick={() => download(doc)}>
                      {icons.download}
                    </button>
                    <button title="Delete" onClick={() => remove(doc)}>
                      {icons.trash}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!visible.length && (
            <Empty
              title="No documents found"
              text="Upload a document or change your search."
            />
          )}
        </section>
      )}
      {uploadOpen && (
        <UploadModal
          close={() => setUploadOpen(false)}
          onDone={() => {
            setUploadOpen(false);
            load();
            notify("Document uploaded");
          }}
        />
      )}
      {versions && (
        <VersionModal
          item={versions}
          close={() => setVersions(null)}
          notify={notify}
          refresh={() =>
            api(`/api/documents/${versions.doc.id}/versions`).then((data) =>
              setVersions({ ...versions, data }),
            )
          }
        />
      )}
    </>
  );
}
function UploadModal({ close, onDone }) {
  const [form, setForm] = useState({ title: "", description: "", file: null });
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    if (!form.file) return;
    setSaving(true);
    const data = new FormData();
    data.append("title", form.title);
    data.append("description", form.description);
    data.append("file", form.file);
    try {
      await api("/api/documents", { method: "POST", body: data });
      onDone();
    } catch (error) {
      alert(errorText(error));
    } finally {
      setSaving(false);
    }
  };
  return (
    <Modal title="Upload document" close={close}>
      <form onSubmit={submit} className="form-grid">
        <label>
          Title
          <input
            required
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
          />
        </label>
        <label>
          Description
          <textarea
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </label>
        <label>
          File
          <input
            required
            type="file"
            onChange={(e) => setForm({ ...form, file: e.target.files[0] })}
          />
        </label>
        <FormButtons close={close} saving={saving} label="Upload" />
      </form>
    </Modal>
  );
}
function VersionModal({ item, close, notify, refresh }) {
  const [file, setFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const upload = async (event) => {
    event.preventDefault();
    if (!file) return;
    setSaving(true);
    const data = new FormData();
    data.append("file", file);
    try {
      await api(`/api/documents/${item.doc.id}/versions`, {
        method: "POST",
        body: data,
      });
      notify("New version uploaded");
      setFile(null);
      refresh();
    } catch (error) {
      notify(errorText(error));
    } finally {
      setSaving(false);
    }
  };
  return (
    <Modal title={`Versions · ${item.doc.title}`} close={close}>
      <div className="version-list">
        {item.data.map((version) => (
          <div className="version-row" key={version.id}>
            <strong>Version {version.versionNumber}</strong>
            <span>{version.originalFilename}</span>
            <small>
              {formatSize(version.sizeBytes)} · {formatDate(version.createdAt)}
            </small>
          </div>
        ))}
      </div>
      <form onSubmit={upload} className="inline-form">
        <input
          required
          type="file"
          onChange={(e) => setFile(e.target.files[0])}
        />
        <button className="primary" disabled={saving}>
          {saving ? "Uploading..." : "Upload version"}
        </button>
      </form>
    </Modal>
  );
}

function Audit({ search }) {
  const [filters, setFilters] = useState({
    action: "",
    entityType: "",
    actorId: "",
    from: "",
    to: "",
  });
  const [data, setData] = useState({ content: [], totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const load = () => {
    setLoading(true);
    const params = new URLSearchParams({ page: "0", size: "50" });
    Object.entries(filters).forEach(
      ([key, value]) => value && params.set(key, value),
    );
    api(`/api/admin/audit-logs?${params}`)
      .then(setData)
      .catch(() => setData({ content: [], totalElements: 0 }))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    load();
  }, []);
  const events = (data.content || []).filter((event) =>
    JSON.stringify(event).toLowerCase().includes(search.toLowerCase()),
  );
  return (
    <>
      <PageIntro
        eyebrow="PROTECTED HISTORY"
        title="Audit trail"
        text="Trace every business action with actor, outcome and before/after values."
        action={
          <button className="secondary" onClick={load}>
            {icons.refresh} Refresh
          </button>
        }
      />
      <section className="panel filters">
        <div>
          <label>
            Action
            <input
              value={filters.action}
              onChange={(e) =>
                setFilters({ ...filters, action: e.target.value })
              }
              placeholder="ROLE_ASSIGNED"
            />
          </label>
          <label>
            Entity type
            <input
              value={filters.entityType}
              onChange={(e) =>
                setFilters({ ...filters, entityType: e.target.value })
              }
              placeholder="DOCUMENT"
            />
          </label>
          <label>
            Actor ID
            <input
              value={filters.actorId}
              onChange={(e) =>
                setFilters({ ...filters, actorId: e.target.value })
              }
            />
          </label>
          <label>
            From
            <input
              type="date"
              value={filters.from}
              onChange={(e) => setFilters({ ...filters, from: e.target.value })}
            />
          </label>
          <label>
            To
            <input
              type="date"
              value={filters.to}
              onChange={(e) => setFilters({ ...filters, to: e.target.value })}
            />
          </label>
          <button className="primary" onClick={load}>
            Apply filters
          </button>
        </div>
      </section>
      {loading ? (
        <Loading title="Loading audit history" />
      ) : (
        <section className="panel table-panel">
          <PanelTitle title={`${data.totalElements || events.length} events`} />
          <table>
            <thead>
              <tr>
                <th>EVENT</th>
                <th>ACTOR</th>
                <th>ENTITY</th>
                <th>OUTCOME</th>
                <th>OCCURRED</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.eventId}>
                  <td>
                    <div className="event-cell">
                      <span className="event-dot" />
                      <div>
                        <strong>{event.action}</strong>
                        <small>{event.eventId}</small>
                      </div>
                    </div>
                  </td>
                  <td>{event.actorUsername || event.actorId}</td>
                  <td>
                    <span className="entity-tag">{event.entityType}</span>{" "}
                    {event.entityId || "—"}
                  </td>
                  <td>
                    <span className={`outcome ${event.outcome?.toLowerCase()}`}>
                      {event.outcome}
                    </span>
                  </td>
                  <td>{formatDate(event.occurredAt)}</td>
                  <td>
                    <button
                      className="text-button"
                      onClick={() => setSelected(event)}
                    >
                      Inspect {icons.arrow}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!events.length && (
            <Empty
              title="No matching events"
              text="Try another filter or perform an operation in the workspace."
            />
          )}
        </section>
      )}
      {selected && (
        <AuditModal event={selected} close={() => setSelected(null)} />
      )}
    </>
  );
}
function AuditModal({ event, close }) {
  return (
    <Modal title={event.action} close={close}>
      <div className="detail-grid">
        <span>Event ID</span>
        <strong>{event.eventId}</strong>
        <span>Actor</span>
        <strong>{event.actorUsername || event.actorId}</strong>
        <span>Occurred</span>
        <strong>{formatDate(event.occurredAt)}</strong>
      </div>
      <h4>Old values</h4>
      <pre>{JSON.stringify(event.oldValues || {}, null, 2)}</pre>
      <h4>New values</h4>
      <pre>{JSON.stringify(event.newValues || {}, null, 2)}</pre>
      <h4>Metadata</h4>
      <pre>{JSON.stringify(event.metadata || {}, null, 2)}</pre>
    </Modal>
  );
}

function Email({ notify, search }) {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState(null);
  const [sendOpen, setSendOpen] = useState(false);
  const [preview, setPreview] = useState(null);
  const load = () => {
    setLoading(true);
    api("/api/admin/email-templates")
      .then(setTemplates)
      .catch((error) => notify(errorText(error)))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    load();
  }, []);
  const filtered = templates.filter((template) =>
    JSON.stringify(template).toLowerCase().includes(search.toLowerCase()),
  );
  const remove = async (template) => {
    if (!window.confirm(`Delete template "${template.name}"?`)) return;
    try {
      await api(
        `/api/admin/email-templates/${encodeURIComponent(template.name)}`,
        { method: "DELETE" },
      );
      notify("Template deleted");
      load();
    } catch (error) {
      notify(errorText(error));
    }
  };
  return (
    <>
      <PageIntro
        eyebrow="COMMUNICATIONS"
        title="Email center"
        text="Manage templates, preview rendered content and send messages through the backend."
        action={
          <div className="button-row">
            <button className="secondary" onClick={() => setSendOpen(true)}>
              {icons.mail} Send email
            </button>
            <button
              className="primary"
              onClick={() => setEditor({ mode: "create" })}
            >
              {icons.plus} New template
            </button>
          </div>
        }
      />
      {loading ? (
        <Loading title="Loading templates" />
      ) : (
        <section className="panel table-panel">
          <PanelTitle title={`${filtered.length} templates`} />
          <table>
            <thead>
              <tr>
                <th>TEMPLATE</th>
                <th>SUBJECT</th>
                <th>STATUS</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filtered.map((template) => (
                <tr key={template.id}>
                  <td>
                    <strong>{template.name}</strong>
                  </td>
                  <td>{template.subjectTemplate}</td>
                  <td>
                    <span className={template.enabled ? "pill green" : "pill"}>
                      {template.enabled ? "Enabled" : "Disabled"}
                    </span>
                  </td>
                  <td className="row-actions">
                    <button
                      onClick={() => setEditor({ mode: "edit", template })}
                    >
                      Edit
                    </button>
                    <button onClick={() => setPreview(template)}>
                      Preview
                    </button>
                    <button onClick={() => remove(template)}>
                      {icons.trash}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!filtered.length && (
            <Empty
              title="No templates"
              text="Create your first template to enable reusable email workflows."
            />
          )}
        </section>
      )}
      {editor && (
        <TemplateModal
          state={editor}
          close={() => setEditor(null)}
          done={() => {
            setEditor(null);
            load();
            notify("Template saved");
          }}
        />
      )}
      {sendOpen && (
        <SendModal
          templates={templates}
          close={() => setSendOpen(false)}
          notify={notify}
        />
      )}
      {preview && (
        <PreviewModal
          template={preview}
          close={() => setPreview(null)}
          notify={notify}
        />
      )}
    </>
  );
}
function TemplateModal({ state, close, done }) {
  const existing = state.template || {};
  const [form, setForm] = useState({
    name: existing.name || "",
    subjectTemplate: existing.subjectTemplate || "",
    htmlContent: "",
    enabled: existing.enabled ?? true,
  });
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await api(
        state.mode === "create"
          ? "/api/admin/email-templates"
          : `/api/admin/email-templates/${encodeURIComponent(existing.name)}`,
        {
          method: state.mode === "create" ? "POST" : "PUT",
          body: JSON.stringify(form),
        },
      );
      done();
    } catch (error) {
      alert(errorText(error));
    } finally {
      setSaving(false);
    }
  };
  return (
    <Modal
      title={state.mode === "create" ? "New template" : `Edit ${existing.name}`}
      close={close}
    >
      <form className="form-grid" onSubmit={submit}>
        <label>
          Name
          <input
            required
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            disabled={state.mode === "edit"}
          />
        </label>
        <label>
          Subject template
          <input
            required
            value={form.subjectTemplate}
            onChange={(e) =>
              setForm({ ...form, subjectTemplate: e.target.value })
            }
          />
        </label>
        <label>
          HTML content
          <textarea
            required
            rows="8"
            value={form.htmlContent}
            onChange={(e) => setForm({ ...form, htmlContent: e.target.value })}
            placeholder="Hello {{name}}"
          />
        </label>
        <label className="check-label">
          <input
            type="checkbox"
            checked={form.enabled}
            onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
          />{" "}
          Enabled
        </label>
        <FormButtons close={close} saving={saving} label="Save template" />
      </form>
    </Modal>
  );
}
function SendModal({ templates, close, notify }) {
  const [form, setForm] = useState({
    to: "",
    cc: "",
    bcc: "",
    subject: "",
    body: "",
    templateName: "",
    variables: {},
  });
  const [saving, setSaving] = useState(false);
  const selectedTemplate = templates.find(
    (template) => template.name === form.templateName,
  );
  const fields = templateVariables(selectedTemplate);
  const update = (key, value) =>
    setForm((current) => ({ ...current, [key]: value }));
  const updateVariable = (key, value) =>
    setForm((current) => ({
      ...current,
      variables: { ...current.variables, [key]: value },
    }));
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        to: form.to
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
        cc: form.cc
          ? form.cc
              .split(",")
              .map((value) => value.trim())
              .filter(Boolean)
          : [],
        bcc: form.bcc
          ? form.bcc
              .split(",")
              .map((value) => value.trim())
              .filter(Boolean)
          : [],
        variables: form.variables,
      };
      await api("/api/admin/emails/send", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      close();
      notify("Email accepted for delivery");
    } catch (error) {
      notify(errorText(error));
    } finally {
      setSaving(false);
    }
  };
  return (
    <Modal title="Send email" close={close}>
      <form className="form-grid" onSubmit={submit}>
        <label>
          Recipients <small>Comma separated</small>
          <input
            required
            value={form.to}
            onChange={(e) => setForm({ ...form, to: e.target.value })}
            placeholder="name@example.com"
          />
        </label>
        <label>
          Template
          <select
            value={form.templateName}
            onChange={(e) => update("templateName", e.target.value)}
          >
            <option value="">Plain email</option>
            {templates.map((template) => (
              <option key={template.id} value={template.name}>
                {template.name}
              </option>
            ))}
          </select>
        </label>
        {selectedTemplate ? (
          <div className="template-fields">
            <div className="template-fields-heading">
              <div>
                <strong>Personalize this template</strong>
                <small>
                  Fill in the fields used by <b>{selectedTemplate.name}</b>.
                </small>
              </div>
              <span className="pill green">
                {fields.length} {fields.length === 1 ? "field" : "fields"}
              </span>
            </div>
            {fields.length ? (
              fields.map((name) => (
                <label key={name}>
                  {variableLabel(name)}
                  <small className="field-token">{`{{${name}}}`}</small>
                  <input
                    required
                    value={form.variables[name] || ""}
                    onChange={(event) =>
                      updateVariable(name, event.target.value)
                    }
                    placeholder={`Enter ${variableLabel(name).toLowerCase()}`}
                  />
                </label>
              ))
            ) : (
              <div className="template-ready">
                <span>{icons.check}</span>
                This template does not require any additional fields.
              </div>
            )}
          </div>
        ) : (
          <>
            <label>
              Subject
              <input
                required
                value={form.subject}
                onChange={(e) => update("subject", e.target.value)}
              />
            </label>
            <label>
              Body
              <textarea
                required
                rows="5"
                value={form.body}
                onChange={(e) => update("body", e.target.value)}
              />
            </label>
          </>
        )}
        <FormButtons close={close} saving={saving} label="Send email" />
      </form>
    </Modal>
  );
}
function PreviewModal({ template, close, notify }) {
  const fields = templateVariables(template);
  const [variables, setVariables] = useState({});
  const [result, setResult] = useState(null);
  const updateVariable = (name, value) =>
    setVariables((current) => ({ ...current, [name]: value }));
  const submit = async (event) => {
    event.preventDefault();
    try {
      setResult(
        await api("/api/admin/email-templates/preview", {
          method: "POST",
          body: JSON.stringify({
            templateName: template.name,
            variables,
          }),
        }),
      );
    } catch (error) {
      notify(errorText(error));
    }
  };
  return (
    <Modal title={`Preview · ${template.name}`} close={close}>
      <form className="form-grid" onSubmit={submit}>
        <div className="template-fields">
          <div className="template-fields-heading">
            <div>
              <strong>Preview with sample data</strong>
              <small>
                Fill in the fields used by <b>{template.name}</b>.
              </small>
            </div>
            <span className="pill green">
              {fields.length} {fields.length === 1 ? "field" : "fields"}
            </span>
          </div>
          {fields.length ? (
            fields.map((name) => (
              <label key={name}>
                {variableLabel(name)}
                <small className="field-token">{`{{${name}}}`}</small>
                <input
                  required
                  value={variables[name] || ""}
                  onChange={(event) => updateVariable(name, event.target.value)}
                  placeholder={`Enter ${variableLabel(name).toLowerCase()}`}
                />
              </label>
            ))
          ) : (
            <div className="template-ready">
              <span>{icons.check}</span>
              This template does not require any additional fields.
            </div>
          )}
        </div>
        <div className="form-actions">
          <button type="button" className="secondary" onClick={close}>
            Cancel
          </button>
          <button className="primary">Render template</button>
        </div>
      </form>
      {result && (
        <div className="preview">
          <strong>{result.subject}</strong>
          <div dangerouslySetInnerHTML={{ __html: result.htmlContent }} />
        </div>
      )}
    </Modal>
  );
}

function Identity({ notify }) {
  const [roles, setRoles] = useState([]);
  const [userId, setUserId] = useState("");
  const [userRoles, setUserRoles] = useState([]);
  const [roleName, setRoleName] = useState("");
  const [loading, setLoading] = useState(true);
  const loadRoles = () =>
    api("/api/admin/roles")
      .then(setRoles)
      .catch((error) => notify(errorText(error)))
      .finally(() => setLoading(false));
  useEffect(() => {
    loadRoles();
  }, []);
  const loadUser = () =>
    userId &&
    api(`/api/admin/users/${encodeURIComponent(userId)}/roles`)
      .then(setUserRoles)
      .catch((error) => notify(errorText(error)));
  const changeRole = async (name, method) => {
    try {
      await api(
        `/api/admin/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(name)}`,
        { method },
      );
      notify(method === "POST" ? "Role assigned" : "Role removed");
      loadUser();
    } catch (error) {
      notify(errorText(error));
    }
  };
  return (
    <>
      <PageIntro
        eyebrow="ACCESS CONTROL"
        title="Identity & roles"
        text="Inspect the role catalog and manage a user's application roles with live protected endpoints."
        action={
          <button className="secondary" onClick={loadRoles}>
            {icons.refresh} Refresh roles
          </button>
        }
      />
      <div className="identity-grid">
        <section className="panel">
          <PanelTitle title="Available roles" />
          {loading ? (
            <Loading title="Loading roles" />
          ) : (
            roles.map((role) => (
              <div className="role-row" key={role.id}>
                <span className="role-icon">{icons.shield}</span>
                <div>
                  <strong>{role.name}</strong>
                  <small>Application role · ID {role.id}</small>
                </div>
                <span className="pill">Available</span>
              </div>
            ))
          )}
        </section>
        <section className="panel">
          <PanelTitle title="User role manager" />
          <label>
            User ID
            <input
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="UUID"
            />
          </label>
          <button className="primary full" onClick={loadUser}>
            Load user roles
          </button>
          {userRoles.length > 0 && (
            <div className="role-list">
              {userRoles.map((role) => (
                <div className="role-row" key={role}>
                  <strong>{role}</strong>
                  <button
                    className="secondary"
                    onClick={() => changeRole(role, "DELETE")}
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
          <div className="inline-form">
            <select
              value={roleName}
              onChange={(e) => setRoleName(e.target.value)}
            >
              <option value="">Assign a role</option>
              {roles.map((role) => (
                <option key={role.id} value={role.name}>
                  {role.name}
                </option>
              ))}
            </select>
            <button
              className="primary"
              disabled={!userId || !roleName}
              onClick={() => changeRole(roleName, "POST")}
            >
              Assign
            </button>
          </div>
        </section>
      </div>
    </>
  );
}

function Assistant({ open, setOpen, page, setPage }) {
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      text: "Hi, I’m your Qeema guide. Tell me what you want to accomplish and I’ll point you to the right page and steps.",
    },
  ]);
  const suggestions = [
    "How do I upload a document?",
    "I want to send an email",
    "Where can I see audit history?",
    "How do I manage user roles?",
  ];
  const answer = (question) => {
    const text = question.toLowerCase();
    if (
      text.includes("upload") ||
      text.includes("document") ||
      text.includes("version")
    ) {
      return {
        text: "Go to Documents. Choose Upload document for a new file, or use the history icon on an existing document to upload a new version. The backend records both actions in the audit trail.",
        page: "Documents",
        action: "Open Documents",
      };
    }
    if (
      text.includes("email") ||
      text.includes("mail") ||
      text.includes("template")
    ) {
      return {
        text: "Go to Email center. You can create or select a template, fill its detected fields, preview the rendered message, and then send it. Plain emails use a subject and body instead.",
        page: "Email center",
        action: "Open Email center",
      };
    }
    if (
      text.includes("audit") ||
      text.includes("history") ||
      text.includes("activity")
    ) {
      return {
        text: "Go to Audit trail. Use the filters to find actions by actor, action, entity type, or date. Select Inspect on an event to view its old values, new values, and metadata.",
        page: "Audit trail",
        action: "Open Audit trail",
      };
    }
    if (
      text.includes("role") ||
      text.includes("user") ||
      text.includes("permission")
    ) {
      return {
        text: "Go to Identity & roles. Load a user by ID to see assigned roles, then choose a role to assign or remove. Role changes are protected and audited.",
        page: "Identity & roles",
        action: "Open Identity & roles",
      };
    }
    if (
      text.includes("overview") ||
      text.includes("dashboard") ||
      text.includes("home")
    ) {
      return {
        text: "The Overview page gives you live workspace counts and recent activity. Use the quick actions to move into Documents, Email center, or Audit trail.",
        page: "Overview",
        action: "Open Overview",
      };
    }
    return {
      text: `I can guide you around the current workspace. You are on ${page}. Try asking about documents, email, templates, audit history, or roles.`,
    };
  };
  const ask = (question) => {
    const trimmed = question.trim();
    if (!trimmed) return;
    const result = answer(trimmed);
    setMessages((current) => [
      ...current,
      { role: "user", text: trimmed },
      result,
    ]);
    setInput("");
  };
  return (
    <>
      <button
        className={`assistant-launcher${open ? " active" : ""}`}
        onClick={() => setOpen(!open)}
        aria-label="Open Qeema guide"
      >
        <span className="assistant-spark">✦</span>
        <span className="assistant-launcher-label">Ask Qeema</span>
      </button>
      {open && (
        <aside className="assistant-panel" aria-label="Qeema guide">
          <div className="assistant-head">
            <div className="assistant-title">
              <span className="assistant-avatar">✦</span>
              <div>
                <strong>Qeema guide</strong>
                <small>Navigation and workflows</small>
              </div>
            </div>
            <button onClick={() => setOpen(false)} aria-label="Close guide">
              {icons.close}
            </button>
          </div>
          <div className="assistant-notice">
            Guide mode is active. I explain steps and navigate only when you
            choose.
          </div>
          <div className="assistant-messages">
            {messages.map((message, index) => (
              <div
                className={`assistant-message ${message.role}`}
                key={`${message.role}-${index}`}
              >
                <p>{message.text}</p>
                {message.action && (
                  <button
                    className="assistant-action"
                    onClick={() => setPage(message.page)}
                  >
                    {message.action} {icons.arrow}
                  </button>
                )}
              </div>
            ))}
          </div>
          <div className="assistant-suggestions">
            {suggestions.map((suggestion) => (
              <button key={suggestion} onClick={() => ask(suggestion)}>
                {suggestion}
              </button>
            ))}
          </div>
          <form
            className="assistant-input"
            onSubmit={(event) => {
              event.preventDefault();
              ask(input);
            }}
          >
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="What are you trying to do?"
              aria-label="Ask Qeema guide"
            />
            <button
              className="primary"
              disabled={!input.trim()}
              aria-label="Ask"
            >
              {icons.arrow}
            </button>
          </form>
        </aside>
      )}
    </>
  );
}

function Modal({ title, close, children }) {
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && close()}
    >
      <div className="modal">
        <div className="modal-head">
          <h3>{title}</h3>
          <button onClick={close}>{icons.close}</button>
        </div>
        {children}
      </div>
    </div>
  );
}
function FormButtons({ close, saving, label }) {
  return (
    <div className="form-actions">
      <button type="button" className="secondary" onClick={close}>
        Cancel
      </button>
      <button className="primary" disabled={saving}>
        {saving ? "Saving..." : label}
      </button>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
