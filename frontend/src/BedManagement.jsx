import { useEffect, useState } from "react";

export default function BedManagement({ hospitalId }) {
  const [buildings, setBuildings] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [beds, setBeds] = useState([]);

  const [buildingName, setBuildingName] = useState("");

  const [roomForm, setRoomForm] = useState({
    buildingId: "",
    departmentId: "",
    floorNumber: "0",
    number: "",
  });

  const [bedForm, setBedForm] = useState({
    roomId: "",
    code: "",
    type: "GENERAL",
    oxygenSupport: false,
    ventilatorCompatible: false,
  });

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const base = `/api/hospitals/${hospitalId}`;

  async function getList(path) {
    const response = await fetch(path);

    if (!response.ok) {
      throw new Error(`Could not load data (HTTP ${response.status})`);
    }

    return response.json();
  }

  async function send(path, method, body) {
    const response = await fetch(path, {
      method,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const details = await response.json().catch(() => ({}));

      throw new Error(
        details.detail ||
          details.message ||
          `Request failed (HTTP ${response.status})`,
      );
    }

    return response.json();
  }

  async function refreshData() {
    if (!hospitalId) return;

    const [buildingData, departmentData, roomData, bedData] = await Promise.all(
      [
        getList(`${base}/buildings`),
        getList(`${base}/departments`),
        getList(`${base}/rooms`),
        getList(`${base}/beds`),
      ],
    );

    setBuildings(buildingData);
    setDepartments(departmentData);
    setRooms(roomData);
    setBeds(bedData);
  }

  useEffect(() => {
    if (!hospitalId) {
      setBuildings([]);
      setDepartments([]);
      setRooms([]);
      setBeds([]);
      return;
    }

    let active = true;

    Promise.all([
      getList(`${base}/buildings`),
      getList(`${base}/departments`),
      getList(`${base}/rooms`),
      getList(`${base}/beds`),
    ])
      .then(([b, d, r, bed]) => {
        if (!active) return;
        setBuildings(b);
        setDepartments(d);
        setRooms(r);
        setBeds(bed);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });

    return () => {
      active = false;
    };
  }, [hospitalId]);

  async function runAction(action, successMessage) {
    setError("");
    setMessage("");

    try {
      await action();
      await refreshData();
      setMessage(successMessage);
    } catch (err) {
      setError(err.message);
    }
  }

  async function addBuilding(event) {
    event.preventDefault();

    await runAction(async () => {
      const created = await send(`${base}/buildings`, "POST", {
        name: buildingName,
      });

      setBuildingName("");
      setRoomForm((prev) => ({
        ...prev,
        buildingId: String(created.id),
      }));
    }, "Building added successfully");
  }

  async function addRoom(event) {
    event.preventDefault();

    await runAction(async () => {
      const created = await send(`${base}/rooms`, "POST", {
        buildingId: Number(roomForm.buildingId),
        departmentId: Number(roomForm.departmentId),
        floorNumber: Number(roomForm.floorNumber),
        number: roomForm.number,
      });

      setRoomForm((prev) => ({
        ...prev,
        number: "",
      }));

      setBedForm((prev) => ({
        ...prev,
        roomId: String(created.id),
      }));
    }, "Room added successfully");
  }

  async function addBed(event) {
    event.preventDefault();

    await runAction(async () => {
      await send(`${base}/beds`, "POST", {
        ...bedForm,
        roomId: Number(bedForm.roomId),
      });

      setBedForm((prev) => ({
        ...prev,
        code: "",
      }));
    }, "Bed added successfully");
  }

  async function toggleMaintenance(bed) {
    const newStatus =
      bed.status === "MAINTENANCE" ? "AVAILABLE" : "MAINTENANCE";

    await runAction(async () => {
      await send(`${base}/beds/${bed.id}/status`, "PATCH", {
        status: newStatus,
      });
    }, `Bed ${bed.code} is now ${newStatus}`);
  }

  if (!hospitalId) {
    return (
      <section className="card">
        <h2>Resource Management</h2>
        <p>Please register a hospital first.</p>
      </section>
    );
  }

  return (
    <div className="resource-management">
      <h2>Buildings, Rooms & Bed Management</h2>
      <p>Register physical hospital locations and manage bed availability.</p>

      {message && <div className="success">{message}</div>}

      {error && <div className="error">{error}</div>}

      {/* BUILDINGS */}

      <section className="card">
        <h2>1. Hospital Buildings</h2>

        <form onSubmit={addBuilding}>
          <input
            placeholder="Building name (e.g. Main Building)"
            value={buildingName}
            onChange={(e) => setBuildingName(e.target.value)}
            required
          />

          <button type="submit">Add Building</button>
        </form>

        <h3>Registered Buildings</h3>

        {buildings.length === 0 ? (
          <p>No buildings registered.</p>
        ) : (
          <div className="resource-list">
            {buildings.map((building) => (
              <div className="resource-item" key={building.id}>
                {building.name}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ROOMS */}

      <section className="card">
        <h2>2. Hospital Rooms</h2>

        <form onSubmit={addRoom}>
          <label>Building</label>
          <select
            value={roomForm.buildingId}
            onChange={(e) =>
              setRoomForm({
                ...roomForm,
                buildingId: e.target.value,
              })
            }
            required
          >
            <option value="">Select Building</option>

            {buildings.map((building) => (
              <option key={building.id} value={building.id}>
                {building.name}
              </option>
            ))}
          </select>

          <label>Department</label>
          <select
            value={roomForm.departmentId}
            onChange={(e) =>
              setRoomForm({
                ...roomForm,
                departmentId: e.target.value,
              })
            }
            required
          >
            <option value="">Select Department</option>

            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </select>

          <label>Floor Number</label>
          <input
            type="number"
            min="0"
            value={roomForm.floorNumber}
            onChange={(e) =>
              setRoomForm({
                ...roomForm,
                floorNumber: e.target.value,
              })
            }
            required
          />

          <label>Room Number</label>
          <input
            placeholder="e.g. ICU-101"
            value={roomForm.number}
            onChange={(e) =>
              setRoomForm({
                ...roomForm,
                number: e.target.value,
              })
            }
            required
          />

          <button
            type="submit"
            disabled={!buildings.length || !departments.length}
          >
            Add Room
          </button>
        </form>

        <h3>Registered Rooms</h3>

        {rooms.length === 0 ? (
          <p>No rooms registered.</p>
        ) : (
          <div className="resource-list">
            {rooms.map((room) => (
              <div className="resource-item" key={room.id}>
                <strong>{room.number}</strong>

                <span>
                  {room.buildingName} — Floor {room.floorNumber}
                </span>

                <span>Department: {room.departmentName}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* BEDS */}

      <section className="card">
        <h2>3. Register Hospital Bed</h2>

        <form onSubmit={addBed}>
          <label>Room</label>
          <select
            value={bedForm.roomId}
            onChange={(e) =>
              setBedForm({
                ...bedForm,
                roomId: e.target.value,
              })
            }
            required
          >
            <option value="">Select Room</option>

            {rooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.number} — {room.buildingName}
              </option>
            ))}
          </select>

          <label>Bed Code</label>
          <input
            placeholder="e.g. ICU-B01"
            value={bedForm.code}
            onChange={(e) =>
              setBedForm({
                ...bedForm,
                code: e.target.value,
              })
            }
            required
          />

          <label>Bed Type</label>
          <select
            value={bedForm.type}
            onChange={(e) =>
              setBedForm({
                ...bedForm,
                type: e.target.value,
              })
            }
          >
            <option value="GENERAL">General</option>
            <option value="EMERGENCY">Emergency</option>
            <option value="ICU">ICU</option>
          </select>

          <label className="bed-checkbox">
            <input
              type="checkbox"
              checked={bedForm.oxygenSupport}
              onChange={(e) =>
                setBedForm({
                  ...bedForm,
                  oxygenSupport: e.target.checked,
                })
              }
            />
            Oxygen support connection
          </label>

          <label className="bed-checkbox">
            <input
              type="checkbox"
              checked={bedForm.ventilatorCompatible}
              onChange={(e) =>
                setBedForm({
                  ...bedForm,
                  ventilatorCompatible: e.target.checked,
                })
              }
            />
            Ventilator compatible
          </label>

          <button type="submit" disabled={!rooms.length}>
            Add Bed
          </button>
        </form>
      </section>

      {/* BED AVAILABILITY */}

      <section className="card">
        <h2>4. Bed Availability</h2>

        <div className="bed-summary">
          <div>
            <strong>{beds.length}</strong>
            <span>Total Beds</span>
          </div>
          <div>
            <strong>
              {beds.filter((b) => b.status === "AVAILABLE").length}
            </strong>
            <span>Available</span>
          </div>
          <div>
            <strong>
              {beds.filter((b) => b.status === "MAINTENANCE").length}
            </strong>
            <span>Maintenance</span>
          </div>
        </div>

        {beds.length === 0 ? (
          <p>No beds registered yet.</p>
        ) : (
          <div className="table-scroll">
            <table className="resource-table">
              <thead>
                <tr>
                  <th>Bed</th>
                  <th>Type</th>
                  <th>Location</th>
                  <th>Status</th>
                  <th>Capabilities</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {beds.map((bed) => (
                  <tr key={bed.id}>
                    <td>{bed.code}</td>
                    <td>{bed.type}</td>

                    <td>
                      {bed.buildingName}
                      <br />
                      Floor {bed.floorNumber}
                      <br />
                      {bed.roomNumber}
                    </td>

                    <td>
                      <span
                        className={`bed-status ${bed.status.toLowerCase()}`}
                      >
                        {bed.status}
                      </span>
                    </td>

                    <td>
                      {bed.oxygenSupport && <div>Oxygen</div>}
                      {bed.ventilatorCompatible && (
                        <div>Ventilator compatible</div>
                      )}
                      {!bed.oxygenSupport && !bed.ventilatorCompatible && (
                        <span>Standard</span>
                      )}
                    </td>

                    <td>
                      {bed.status === "OCCUPIED" ? (
                        <span>Assigned</span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => toggleMaintenance(bed)}
                        >
                          {bed.status === "MAINTENANCE"
                            ? "Mark Available"
                            : "Mark Maintenance"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
