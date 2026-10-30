const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

export async function fetchWalletBalance() {
  const response = await fetch(`${API_BASE_URL}/wallet/balance`, {
    credentials: "include",
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }

  if (!response.ok) {
    const responseBody = (await response.text()).trim();
    throw new Error(responseBody || "Could not load your wallet. Please try again.");
  }

  const wallet = await response.json();
  if (!Number.isFinite(wallet.balance)) {
    throw new Error("The server returned an invalid wallet balance.");
  }

  return wallet.balance;
}

export async function submitAuthentication(mode, request) {
  const endpoint = mode === "register" ? "/register" : "/login";
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  const responseBody = (await response.text()).trim();

  if (!response.ok) {
    throw new Error(responseBody || "Something went wrong. Please try again.");
  }

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
  const response = await fetch(`${API_BASE_URL}/transactions/${action}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }

  const responseBody = (await response.text()).trim();
  if (!response.ok) {
    throw new Error(responseBody || `Could not ${action} money. Please try again.`);
  }

  const balance = await fetchWalletBalance();
  return balance === null ? null : { balance };
}
