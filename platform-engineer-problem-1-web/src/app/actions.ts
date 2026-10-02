"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { platform, TOKEN_COOKIE, USER_COOKIE } from "@/lib/platform";

export type AuthState = { error?: string };

export async function signIn(_previous: AuthState, formData: FormData): Promise<AuthState> {
    const email = String(formData.get("email") ?? "").trim();
    const password = String(formData.get("password") ?? "");
    if (!email || !password) {
        return { error: "Email and password are required." };
    }

    let token: string | undefined;
    try {
        if (formData.get("intent") === "register") {
            const created = await platform("/api/register", {
                method: "POST",
                body: { name: email.split("@")[0], email, password },
                token: "",
            });
            if (!created.ok) {
                const detail = (created.body as { detail?: string } | null)?.detail;
                return { error: detail ?? "Could not create the account." };
            }
        }
        const auth = await platform("/api/auth", { method: "POST", body: { email, password }, token: "" });
        if (!auth.ok) {
            return { error: "Invalid email or password." };
        }
        token = (auth.body as { accessToken?: string } | null)?.accessToken;
    } catch {
        return { error: "The platform API is not reachable." };
    }
    if (!token) {
        return { error: "The platform did not return a token." };
    }

    const store = await cookies();
    // httpOnly: el JavaScript del navegador no puede leer el token
    const options = { httpOnly: true, sameSite: "lax" as const, path: "/", maxAge: 60 * 60 };
    store.set(TOKEN_COOKIE, token, options);
    store.set(USER_COOKIE, email, options);
    redirect("/");
}

export async function signOut(): Promise<void> {
    const store = await cookies();
    store.delete(TOKEN_COOKIE);
    store.delete(USER_COOKIE);
    redirect("/login");
}