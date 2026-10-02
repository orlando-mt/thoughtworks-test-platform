import type Anthropic from "@anthropic-ai/sdk";
import { cookies } from "next/headers";
import { runAgent } from "@/lib/agent";
import { platform, TOKEN_COOKIE, type ModuleDetail } from "@/lib/platform";

export async function POST(request: Request) {
    const token = (await cookies()).get(TOKEN_COOKIE)?.value;
    if (!token) {
        return Response.json({ error: "Not signed in." }, { status: 401 });
    }

    const { moduleId, history, message } = await request.json();
    if (typeof moduleId !== "string" || typeof message !== "string" || !message.trim()) {
        return Response.json({ error: "moduleId and message are required." }, { status: 400 });
    }

    const detail = await platform(`/api/modules/${encodeURIComponent(moduleId)}`, { token });
    if (detail.status === 401) {
        return Response.json({ error: "Session expired." }, { status: 401 });
    }
    if (!detail.ok) {
        return Response.json({ error: "The module is not in the catalog." }, { status: 404 });
    }

    try {
        const result = await runAgent(
            detail.body as ModuleDetail,
            Array.isArray(history) ? (history as Anthropic.MessageParam[]) : [],
            message.trim(),
            token,
        );
        return Response.json(result);
    } catch (error) {
        console.error("Agent failed", error);
        return Response.json({ error: "The assistant failed. Check the server log." }, { status: 502 });
    }
}