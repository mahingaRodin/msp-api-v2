import http from "k6/http";

export const BASE = __ENV.BASE_URL || "http://localhost:5000";

export function jsonHeaders(token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return { headers };
}

export function login(email, password) {
  if (!email || !password) {
    throw new Error("Set EMAIL and PASSWORD (k6 -e EMAIL=... -e PASSWORD=...)");
  }
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email, password }),
    jsonHeaders(),
  );
  if (res.status !== 200) {
    throw new Error(`Login failed ${res.status}: ${res.body}`);
  }
  const body = res.json();
  const token = body.jwt || body.token;
  if (!token) {
    throw new Error(`Login response had no jwt: ${res.body}`);
  }
  return { token, user: body.user || {} };
}

export function catalogSnapshot() {
  const products = http.get(`${BASE}/api/catalog/products?page=0&size=12&inStock=true`);
  const branches = http.get(`${BASE}/api/catalog/branches?size=20`);
  const productList =
    products.status === 200 ? products.json("content") || [] : [];
  const branchList =
    branches.status === 200 ? branches.json("content") || [] : [];
  return {
    productId: productList[0] && productList[0].id,
    branchId: branchList[0] && branchList[0].id,
  };
}
