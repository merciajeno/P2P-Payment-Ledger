import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

function getErrorMessage(error, fallback) {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : fallback;
  }

  const responseBody = error.response?.data;
  if (typeof responseBody === "string" && responseBody.trim()) {
    return responseBody.trim();
  }
  if (responseBody && typeof responseBody.message === "string") {
    return responseBody.message;
  }
  return error.response ? fallback : "Could not reach the server. Check that the ledger API is running and try again.";
}

export async function fetchWalletBalance() {
  try {
    const response = await api.get("/wallet/balance");
    const balance = Number(response.data?.balance);
    if (!Number.isFinite(balance)) {
      throw new Error("The server returned an invalid wallet balance.");
    }
    return balance;
  } catch (error) {
    if (axios.isAxiosError(error) && [401, 403].includes(error.response?.status)) {
      return null;
    }
    throw new Error(getErrorMessage(error, "Could not load your wallet. Please try again."));
  }
}

export async function fetchCurrentUser() {
  try {
    const response = await api.get("/me");
    const user = response.data;
    if (!user || typeof user.name !== "string" || !["ADMIN", "USER"].includes(user.role)) {
      throw new Error("The server returned an invalid user session.");
    }
    return user;
  } catch (error) {
    if (axios.isAxiosError(error) && [401, 403].includes(error.response?.status)) {
      return null;
    }
    throw new Error(getErrorMessage(error, "Could not verify your session."));
  }
}

export async function submitAuthentication(mode, request) {
  const endpoint = mode === "register" ? "/register" : "/login";
  let response;
  try {
    response = await api.post(endpoint, request);
  } catch (error) {
    throw new Error(getErrorMessage(error, "Something went wrong. Please try again."));
  }
  const responseBody = typeof response.data === "string" ? response.data.trim() : "";

  if (mode === "register") {
    return { responseBody };
  }

  const user = await fetchCurrentUser();
  if (user === null) {
    throw new Error("Could not verify your session. Please sign in again.");
  }

  const balance = user.role === "USER" ? await fetchWalletBalance() : null;
  if (user.role === "USER" && balance === null) {
    throw new Error("Could not load your wallet. Please sign in again.");
  }

  return { balance, user };
}

export async function logoutUser() {
  try {
    await api.post("/logout");
  } catch (error) {
    throw new Error(getErrorMessage(error, "Could not log out. Please try again."));
  }
}

export async function submitWalletAction(action, amount) {
  try {
    await api.post(`/transactions/${action}`, { amount });
  } catch (error) {
    if (axios.isAxiosError(error) && [401, 403].includes(error.response?.status)) {
      return null;
    }
    throw new Error(getErrorMessage(error, `Could not ${action} money. Please try again.`));
  }

  const balance = await fetchWalletBalance();
  return balance === null ? null : { balance };
}

export async function submitTransfer(requestBody) {
  try {
    const response = await api.post("/transactions", requestBody, {
      headers: { "Content-Type": "application/json" },
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const { status } = error.response;
      const message = getErrorMessage(error, "Could not send money. Please try again.");
      if (status >= 500 || [408, 425].includes(status)) {
        return { uncertainError: true, message };
      }
      return { definitiveError: true, status, message };
    }

    return {
      uncertainError: true,
      message: getErrorMessage(error, "The payment result could not be confirmed."),
    };
  }
}

export async function fetchLedgerHistory(page = 0) {
  try {
    const response = await api.get("/api/ledger/history", { params: { page } });
    return response.data;
  } catch (error) {
    throw new Error(getErrorMessage(error, "Could not load transaction history."));
  }
}

export async function searchUser(query) {
  try {
    const response = await api.get("/users/search", { params: { query } });
    const user = response.data;
    if (!user || typeof user.name !== "string" || typeof user.email !== "string") {
      throw new Error("The server returned an invalid user search result.");
    }
    return user;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return null;
    }
    throw new Error(getErrorMessage(error, "Could not search for this user."));
  }
}

export async function fetchUnreadCount() {
  try {
    const response = await api.get("/notifications/unread-count");
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && [401, 403].includes(error.response?.status)) {
      return null;
    }
    return 0;
  }
}

export async function fetchNotifications() {
  try {
    const response = await api.get("/notifications");
    return response.data;
  } catch (error) {
    throw new Error(getErrorMessage(error, "Could not load notifications."));
  }
}

export async function markNotificationsRead() {
  try {
    await api.post("/notifications/mark-read");
  } catch (error) {
    // non-critical, fail silently
  }
}

async function fetchAdminData(path, fallback) {
  try {
    const response = await api.get(path);
    return response.data;
  } catch (error) {
    throw new Error(getErrorMessage(error, fallback));
  }
}

async function postAdminAction(path, fallback) {
  try {
    const response = await api.post(path);
    return response.data;
  } catch (error) {
    throw new Error(getErrorMessage(error, fallback));
  }
}

export function fetchAdminDashboard() {
  return fetchAdminData("/admin/dashboard", "Could not load the admin dashboard.");
}

export function fetchAdminUsers() {
  return fetchAdminData("/admin/users", "Could not load users.");
}

export function fetchAdminTransactions(page = 0) {
  return fetchAdminData(`/admin/transactions?page=${page}`, "Could not load transactions.");
}

export function fetchPendingSettlements() {
  return fetchAdminData("/admin/settlements/pending", "Could not load pending settlements.");
}

export function fetchFraudAlerts() {
  return fetchAdminData("/admin/fraud-alerts", "Could not load fraud alerts.");
}

export function fetchRecentJobs() {
  return fetchAdminData("/admin/jobs", "Could not load settlement jobs.");
}

export function freezeWallet(userId) {
  return postAdminAction(`/admin/users/${userId}/freeze`, "Could not freeze the wallet.");
}

export function unfreezeWallet(userId) {
  return postAdminAction(`/admin/users/${userId}/unfreeze`, "Could not unfreeze the wallet.");
}

export function manualSettle(transactionId) {
  return postAdminAction(`/admin/settlements/${transactionId}/settle`, "Could not settle the transaction.");
}

export function blockFraudAlert(alertId) {
  return postAdminAction(`/admin/fraud-alerts/${alertId}/block`, "Could not block the transaction.");
}

export function dismissFraudAlert(alertId) {
  return postAdminAction(`/admin/fraud-alerts/${alertId}/dismiss`, "Could not dismiss the alert.");
}
