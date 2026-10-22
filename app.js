
const contextPath = window.location.pathname.substring(
    0,
    window.location.pathname.indexOf("/", 1)
);

// ==================== API HELPER ====================

async function api(url, options = {}) {
    const response = await fetch(contextPath + url, {
        credentials: "same-origin",
        ...options
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = contextPath + "/login.html";
        throw new Error("Please log in again.");
    }

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Request failed.");
    }

    if (response.status === 204) return null;

    const contentType = response.headers.get("content-type") || "";

    return contentType.includes("application/json")
        ? response.json()
        : null;
}

// ==================== COMMON FUNCTIONS ====================

function logout() {
    window.location.href = contextPath + "/logout";
}

function money(value) {
    return Number(value || 0).toLocaleString("en-IN", {
        style: "currency",
        currency: "INR"
    });
}

function cell(value) {
    const td = document.createElement("td");
    td.textContent = value == null ? "" : String(value);
    return td;
}

function actionButton(label, action, id, className = "small-btn") {
    const button = document.createElement("button");

    button.type = "button";
    button.textContent = label;
    button.className = className;
    button.addEventListener("click", () => action(id));

    return button;
}

// ==================== DASHBOARD ====================

async function loadDashboard() {
    if (!document.getElementById("customerCount")) return;

    try {
        const data = await api("/api/dashboard");

        document.getElementById("customerCount").textContent = data.customers;
        document.getElementById("billCount").textContent = data.bills;
        document.getElementById("paidAmount").textContent = money(data.paid);
        document.getElementById("unpaidAmount").textContent = money(data.unpaid);

    } catch (error) {
        const element = document.getElementById("dashboardError");
        if (element) element.textContent = error.message;
        console.error("Dashboard error:", error);
    }
}

// ==================== CUSTOMERS ====================

let customers = [];

async function loadCustomers() {
    // Do not return early on the Bills page.
    // The Bills page also needs the customer list.
    const tbody = document.getElementById("customerRows");

    try {
        customers = await api("/api/customers");

        if (!Array.isArray(customers)) {
            throw new Error("Invalid customer data returned by server.");
        }

        // Render the customer table only if it exists.
        if (tbody) {
            renderCustomers();
        }

        // Populate the bill form dropdown on either page.
        populateCustomerSelect();

    } catch (error) {
        console.error("Could not load customers:", error);

        if (tbody) {
            const tr = document.createElement("tr");
            const td = cell("Could not load customers: " + error.message);

            td.colSpan = 7;
            tr.appendChild(td);
            tbody.replaceChildren(tr);
        }

        const select = document.getElementById("billCustomerId");

        if (select) {
            select.replaceChildren(
                new Option("Failed to load customers", "")
            );
        }
    }
}

function renderCustomers() {
    const tbody = document.getElementById("customerRows");
    if (!tbody) return;

    const search = (
        document.getElementById("customerSearch")?.value || ""
    ).toLowerCase();

    tbody.replaceChildren();

    const filtered = customers.filter(c =>
        [c.id, c.name, c.meterNumber, c.phone, c.email, c.address]
            .some(value =>
                String(value || "").toLowerCase().includes(search)
            )
    );

    if (filtered.length === 0) {
        const tr = document.createElement("tr");
        const td = cell("No customer records found.");

        td.colSpan = 7;
        tr.appendChild(td);
        tbody.appendChild(tr);
        return;
    }

    filtered.forEach(c => {
        const tr = document.createElement("tr");

        [c.id, c.name, c.meterNumber, c.phone, c.email, c.address]
            .forEach(value => tr.appendChild(cell(value)));

        const actions = document.createElement("td");

        actions.append(
            actionButton("Edit", editCustomer, c.id),
            actionButton(
                "Delete",
                deleteCustomer,
                c.id,
                "small-btn danger"
            )
        );

        tr.appendChild(actions);
        tbody.appendChild(tr);
    });
}

function resetCustomerForm() {
    const form = document.getElementById("customerForm");
    if (!form) return;

    form.reset();

    document.getElementById("customerId").value = "";
    document.getElementById("customerFormTitle").textContent = "Add Customer";
    document.getElementById("customerMessage").textContent = "";
}

function editCustomer(id) {
    const c = customers.find(item => String(item.id) === String(id));
    if (!c) return;

    document.getElementById("customerId").value = c.id;
    document.getElementById("name").value = c.name || "";
    document.getElementById("meterNumber").value = c.meterNumber || "";
    document.getElementById("address").value = c.address || "";
    document.getElementById("phone").value = c.phone || "";
    document.getElementById("email").value = c.email || "";

    document.getElementById("customerFormTitle").textContent = "Edit Customer";
    document.getElementById("customerMessage").textContent = "";

    window.scrollTo({ top: 0, behavior: "smooth" });
}

async function deleteCustomer(id) {
    if (!confirm("Delete this customer?")) return;

    try {
        await api("/api/customers?id=" + encodeURIComponent(id), {
            method: "DELETE"
        });

        await loadCustomers();
        await loadBills();
        await loadDashboard();

    } catch (error) {
        alert("Could not delete customer: " + error.message);
    }
}

// SAVE OR UPDATE CUSTOMER

document.getElementById("customerForm")?.addEventListener(
    "submit",
    async event => {
        event.preventDefault();

        const message = document.getElementById("customerMessage");

        const data = new URLSearchParams({
            id: document.getElementById("customerId").value,
            name: document.getElementById("name").value.trim(),
            meterNumber: document.getElementById("meterNumber").value.trim(),
            address: document.getElementById("address").value.trim(),
            phone: document.getElementById("phone").value.trim(),
            email: document.getElementById("email").value.trim()
        });

        if (!data.get("name") || !data.get("meterNumber") ||
            !data.get("address")) {
            message.textContent =
                "Name, meter number and address are required.";
            return;
        }

        message.textContent = "Saving customer...";

        try {
            const response = await fetch(contextPath + "/api/customers", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: data.toString(),
                credentials: "same-origin"
            });

            if (response.status === 401 || response.status === 403) {
                window.location.href = contextPath + "/login.html";
                return;
            }

            const text = await response.text();
            let result = {};

            try {
                result = text ? JSON.parse(text) : {};
            } catch {
                throw new Error("Unexpected server response. Check Tomcat logs.");
            }

            if (!response.ok) {
                throw new Error(result.message || "Customer save failed.");
            }

            message.textContent =
                result.message || "Customer saved successfully.";

            resetCustomerForm();

            await loadCustomers();
            await loadDashboard();

        } catch (error) {
            message.textContent = "Error: " + error.message;
            console.error("Customer save error:", error);
        }
    }
);

document.getElementById("customerSearch")?.addEventListener(
    "input",
    renderCustomers
);

// ==================== BILL CUSTOMER DROPDOWN ====================

function populateCustomerSelect() {
    const select = document.getElementById("billCustomerId");

    if (!select) {
        console.log("This page has no bill customer dropdown.");
        return;
    }

    select.replaceChildren(
        new Option("Select a customer", "")
    );

    customers.forEach(customer => {
        select.add(
            new Option(
                customer.name + " — " + customer.meterNumber,
                String(customer.id)
            )
        );
    });

    console.log("Customers loaded into dropdown:", customers.length);
}

// ==================== BILLS ====================

let bills = [];

async function loadBills() {
    if (!document.getElementById("billRows")) return;

    const tbody = document.getElementById("billRows");

    try {
        bills = await api("/api/bills");

        if (!Array.isArray(bills)) {
            throw new Error("Invalid bill data returned by server.");
        }

        renderBills();

    } catch (error) {
        const tr = document.createElement("tr");
        const td = cell("Could not load bills: " + error.message);

        td.colSpan = 8;
        tr.appendChild(td);
        tbody.replaceChildren(tr);

        console.error("Bill loading error:", error);
    }
}

function renderBills() {
    const tbody = document.getElementById("billRows");
    if (!tbody) return;

    const search = (
        document.getElementById("billSearch")?.value || ""
    ).toLowerCase();

    tbody.replaceChildren();

    const filtered = bills.filter(b =>
        [b.id, b.name, b.meterNumber, b.units, b.month, b.status]
            .some(value =>
                String(value || "").toLowerCase().includes(search)
            )
    );

    if (filtered.length === 0) {
        const tr = document.createElement("tr");
        const td = cell("No bills found.");

        td.colSpan = 8;
        tr.appendChild(td);
        tbody.appendChild(tr);
        return;
    }

    filtered.forEach(b => {
        const tr = document.createElement("tr");

        [b.id, b.name, b.meterNumber, b.units, b.month, money(b.amount), b.status]
            .forEach(value => tr.appendChild(cell(value)));

        const actions = document.createElement("td");

        actions.append(
            actionButton(
                b.status === "PAID" ? "Mark Unpaid" : "Mark Paid",
                () => updateBillStatus(
                    b.id,
                    b.status === "PAID" ? "UNPAID" : "PAID"
                ),
                b.id
            ),
            actionButton("Delete", deleteBill, b.id, "small-btn danger")
        );

        tr.appendChild(actions);
        tbody.appendChild(tr);
    });
}

async function updateBillStatus(id, status) {
    const data = new URLSearchParams({
        action: "status",
        id: String(id),
        status: status
    });

    try {
        const response = await fetch(contextPath + "/api/bills", {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: data.toString(),
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Bill update failed.");
        }

        await loadBills();
        await loadDashboard();

    } catch (error) {
        alert("Could not update bill: " + error.message);
    }
}

async function deleteBill(id) {
    if (!confirm("Delete this bill?")) return;

    try {
        await api("/api/bills?id=" + encodeURIComponent(id), {
            method: "DELETE"
        });

        await loadBills();
        await loadDashboard();

    } catch (error) {
        alert("Could not delete bill: " + error.message);
    }
}

// GENERATE BILL

document.getElementById("billForm")?.addEventListener(
    "submit",
    async event => {
        event.preventDefault();

        const form = event.currentTarget;
        const message = document.getElementById("billMessage");
        const data = new URLSearchParams(new FormData(form));

        try {
            const response = await fetch(contextPath + "/api/bills", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: data.toString(),
                credentials: "same-origin"
            });

            if (response.status === 401 || response.status === 403) {
                window.location.href = contextPath + "/login.html";
                return;
            }

            if (!response.ok) {
                throw new Error(
                    await response.text() || "Bill generation failed."
                );
            }

            message.textContent = "Bill generated successfully.";
            form.reset();

            await loadBills();
            await loadDashboard();

            // Restore the customer options after resetting the form.
            populateCustomerSelect();

        } catch (error) {
            message.textContent = "Error: " + error.message;
            console.error("Bill generation error:", error);
        }
    }
);

document.getElementById("billSearch")?.addEventListener(
    "input",
    renderBills
);

// ==================== INITIALIZE ====================

document.addEventListener("DOMContentLoaded", async () => {
    await loadCustomers();
    await loadDashboard();
    await loadBills();
});
