"use client";

import { useActionState } from "react";
import { signIn, type AuthState } from "@/app/actions";

const initialState: AuthState = {};

export default function LoginPage() {
    const [state, action, pending] = useActionState(signIn, initialState);

    return (
        <main className="flex flex-1 items-center justify-center px-6">
            <form action={action} className="w-full max-w-sm space-y-5">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Thoughtworks Test</h1>
                    <p className="mt-1 text-sm opacity-70">Sign in to request infrastructure from the catalog.</p>
                </div>

                <label className="block text-sm">
                    Email
                    <input
                        name="email"
                        type="email"
                        required
                        autoComplete="email"
                        className="mt-1 w-full rounded-md border border-black/15 bg-transparent px-3 py-2 dark:border-white/20"
                    />
                </label>

                <label className="block text-sm">
                    Password
                    <input
                        name="password"
                        type="password"
                        required
                        autoComplete="current-password"
                        className="mt-1 w-full rounded-md border border-black/15 bg-transparent px-3 py-2 dark:border-white/20"
                    />
                </label>

                {state.error && (
                    <p role="alert" className="text-sm text-red-600 dark:text-red-400">
                        {state.error}
                    </p>
                )}

                <div className="flex gap-3">
                    <button
                        name="intent"
                        value="login"
                        disabled={pending}
                        className="flex-1 rounded-md bg-foreground px-4 py-2 text-sm font-medium text-background disabled:opacity-50"
                    >
                        {pending ? "Signing in…" : "Sign in"}
                    </button>
                    <button
                        name="intent"
                        value="register"
                        disabled={pending}
                        className="flex-1 rounded-md border border-black/15 px-4 py-2 text-sm font-medium disabled:opacity-50 dark:border-white/20"
                    >
                        Create account
                    </button>
                </div>
            </form>
        </main>
    );
}