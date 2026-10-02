import { cookies } from "next/headers";

const API_URL = process.env.PLATFORM_API_URL ?? "http://localhost:8080";

export const TOKEN_COOKIE = "kp_token";
export const USER_COOKIE = "kp_user";

export type ApiResult = { status: number; ok: boolean; body: unknown };

export type ModuleSummary = {
    id: string;
    displayName: string;
    description: string;
    version: string;
};

export type ModuleDetail = ModuleSummary & {
    source: string;
    schema: unknown;
    enforced: Record<string, unknown>;
    outputs: { name: string; description?: string }[];
};

/** Llama a la API de Java desde el servidor de Next.js. El token sale de la cookie salvo que se pase uno. */
export async function platform(
    path: string,
    init: { method?: string; body?: unknown; token?: string } = {},
): Promise<ApiResult> {
    const token = init.token ?? (await cookies()).get(TOKEN_COOKIE)?.value;
    const headers: Record<string, string> = { Accept: "application/json" };
    if (init.body !== undefined) headers["Content-Type"] = "application/json";
    if (token) headers.Authorization = `Bearer ${token}`;

    const response = await fetch(`${API_URL}${path}`, {
        method: init.method ?? "GET",
        headers,
        body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
        cache: "no-store",
    });

    const text = await response.text();
    let body: unknown = null;
    try {
        body = text ? JSON.parse(text) : null;
    } catch {
        body = text;
    }
    return { status: response.status, ok: response.ok, body };
}