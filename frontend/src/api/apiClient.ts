// Thin fetch wrapper for the Spring Boot REST API (base URL from env config).

export async function apiGet<T>(path: string): Promise<T> {
  // TODO: fetch(baseUrl + path), attach JWT from authStore, handle errors
  throw new Error("TODO: implement apiGet");
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  // TODO: fetch(baseUrl + path, { method: "POST", body: JSON.stringify(body) })
  throw new Error("TODO: implement apiPost");
}
