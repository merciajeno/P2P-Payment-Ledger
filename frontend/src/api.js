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

  const balance = await fetchWalletBalance();
  if (balance === null) {
    throw new Error("Could not verify your session. Please sign in again.");
  }

  return { balance };
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
