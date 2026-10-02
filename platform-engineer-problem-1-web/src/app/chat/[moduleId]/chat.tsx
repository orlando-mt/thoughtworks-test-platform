"use client";

import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";

type Step = { tool: string; label: string; detail: string; ok: boolean };
type Item = { role: "user" | "assistant"; text: string; steps?: Step[]; error?: boolean };

function linkify(text: string): ReactNode[] {
    return text.split(/(https?:\/\/[^\s)]+)/g).map((part, index) =>
        /^https?:\/\//.test(part) ? (
            <a key={index} href={part} target="_blank" rel="noreferrer" className="underline underline-offset-4">
                {part}
            </a>
        ) : (
            part
        ),
    );
}

export function Chat({ moduleId, displayName }: { moduleId: string; displayName: string }) {
    const [items, setItems] = useState<Item[]>([
        { role: "assistant", text: `You picked ${displayName}. Which project is this for?` },
    ]);
    const [history, setHistory] = useState<unknown[]>([]);
    const [input, setInput] = useState("");
    const [busy, setBusy] = useState(false);
    const bottom = useRef<HTMLDivElement>(null);

    useEffect(() => {
        bottom.current?.scrollIntoView({ behavior: "smooth" });
    }, [items, busy]);

    async function send(event: FormEvent) {
        event.preventDefault();
        const message = input.trim();
        if (!message || busy) return;

        setInput("");
        setItems((current) => [...current, { role: "user", text: message }]);
        setBusy(true);
        try {
            const response = await fetch("/api/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ moduleId, history, message }),
            });
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            const data = await response.json();
            if (!response.ok) {
                setItems((current) => [...current, { role: "assistant", text: data.error ?? "Something went wrong.", error: true }]);
                return;
            }
            setHistory(data.history);
            setItems((current) => [...current, { role: "assistant", text: data.reply, steps: data.steps }]);
        } catch {
            setItems((current) => [...current, { role: "assistant", text: "The assistant is not reachable.", error: true }]);
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className="mt-6 flex flex-1 flex-col rounded-lg border border-black/10 dark:border-white/15">
            <div className="flex-1 space-y-4 overflow-y-auto p-5" aria-live="polite">
                {items.map((item, index) => (
                    <div key={index} className={item.role === "user" ? "flex justify-end" : "flex justify-start"}>
                        <div className="max-w-[85%] space-y-2">
                            {item.steps && item.steps.length > 0 && (
                                <ul className="space-y-1 font-mono text-xs opacity-70">
                                    {item.steps.map((step, stepIndex) => (
                                        <li key={stepIndex}>
                      <span className={step.ok ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"}>
                        {step.ok ? "✓" : "✗"}
                      </span>{" "}
                                            {step.label} <span className="opacity-60">({step.detail})</span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                            <p
                                className={
                                    "whitespace-pre-wrap rounded-lg px-4 py-2 text-sm " +
                                    (item.role === "user"
                                        ? "bg-foreground text-background"
                                        : item.error
                                            ? "border border-red-500/50 text-red-600 dark:text-red-400"
                                            : "border border-black/10 dark:border-white/15")
                                }
                            >
                                {linkify(item.text)}
                            </p>
                        </div>
                    </div>
                ))}
                {busy && <p className="text-sm opacity-60">Working… generating Terraform can take up to 30 seconds.</p>}
                <div ref={bottom} />
            </div>

            <form onSubmit={send} className="flex gap-3 border-t border-black/10 p-4 dark:border-white/15">
                <input
                    value={input}
                    onChange={(event) => setInput(event.target.value)}
                    placeholder="Type your answer…"
                    aria-label="Message"
                    autoFocus
                    className="flex-1 rounded-md border border-black/15 bg-transparent px-3 py-2 text-sm dark:border-white/20"
                />
                <button
                    disabled={busy || !input.trim()}
                    className="rounded-md bg-foreground px-4 py-2 text-sm font-medium text-background disabled:opacity-50"
                >
                    Send
                </button>
            </form>
        </section>
    );
}