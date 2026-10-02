import Anthropic from "@anthropic-ai/sdk";
import { platform, type ApiResult, type ModuleDetail } from "@/lib/platform";

export type Step = { tool: string; label: string; detail: string; ok: boolean };

const client = new Anthropic(); // lee ANTHROPIC_API_KEY del entorno del servidor
const MODEL = process.env.ANTHROPIC_MODEL ?? "claude-haiku-4-5-20251001";
const MAX_TURNS = 8;

const TOOLS: Anthropic.Tool[] = [
    {
        name: "list_projects",
        description: "List the projects owned by the user.",
        input_schema: { type: "object", properties: {} },
    },
    {
        name: "get_project",
        description: "Get a project by name. Returns 404 if it does not exist or belongs to someone else.",
        input_schema: {
            type: "object",
            properties: { name: { type: "string", description: "Project name, e.g. pagos" } },
            required: ["name"],
        },
    },
    {
        name: "create_project",
        description: "Create a project. This creates the private <name>-infra repository, which already includes the pipeline.",
        input_schema: {
            type: "object",
            properties: { name: { type: "string" } },
            required: ["name"],
        },
    },
    {
        name: "prepare_module",
        description:
            "Prepare the project for the selected module: the platform generates the Terraform files with a model, validates them and commits them to the repository. Safe to call again: it does nothing if the project already has the module. Can take up to 30 seconds.",
        input_schema: {
            type: "object",
            properties: { project: { type: "string" } },
            required: ["project"],
        },
    },
    {
        name: "create_resource",
        description:
            "Request a resource of the selected module. The platform validates the values and policies, commits the tfvars to main and the pipeline applies it. Only call it after the user confirmed the summary.",
        input_schema: {
            type: "object",
            properties: {
                project: { type: "string" },
                environment: { type: "string" },
                name: { type: "string", description: "Short name chosen by the user" },
                inputs: { type: "object", description: "Only the configurable inputs of the module" },
            },
            required: ["project", "environment", "name"],
        },
    },
    {
        name: "list_resources",
        description: "List the resources already requested for a project, with their status.",
        input_schema: {
            type: "object",
            properties: { project: { type: "string" } },
            required: ["project"],
        },
    },
];

const LABELS: Record<string, string> = {
    list_projects: "Listing your projects",
    get_project: "Looking up the project",
    create_project: "Creating the repository from the template",
    prepare_module: "Generating the Terraform files and committing them",
    create_resource: "Committing the tfvars and triggering the pipeline",
    list_resources: "Listing the project resources",
};

function systemPrompt(module: ModuleDetail): string {
    return `You are the assistant of Kordanix's internal developer platform. You help a developer provision infrastructure through the platform API. You never touch AWS or GitHub yourself: you only call the tools, and the platform enforces every rule.

The user picked this module from the catalog:
${JSON.stringify(module, null, 2)}

How the request works:
1. The UI already greeted the user and asked which project this is for, so the first message is usually the project name. Call get_project. If it does not exist, ask the user to confirm before calling create_project.
2. Call prepare_module for that project. Tell the user in one sentence what it did.
3. Ask for the environment, a short name and the configurable inputs (the ones under schema.properties.inputs). Mention the default of each input so the user can accept it. The short name uses lowercase letters, numbers and hyphens, 3 to 22 characters; the platform builds the real name as <project>-<environment>-<name>-<suffix>.
4. The "enforced" values are platform policies. Explain them if asked, but never offer to change them.
5. Show a short summary and wait for an explicit confirmation before calling create_resource.
6. After create_resource, report the real resource name, the status and the pipeline URL. The status stays APPLYING while the pipeline runs.

Rules:
- Ask one thing at a time and keep answers short.
- Never invent values, names or results. Only report what a tool returned.
- If a tool returns an error, explain it plainly using the "detail", "errors" or "policy" fields, and say what the user can do next.
- Reply in the language the user writes in.
- Plain text only: no markdown, no bullet symbols, no bold.`;
}

async function callTool(
    name: string,
    input: Record<string, unknown>,
    moduleId: string,
    token: string,
): Promise<{ ok: boolean; content: string; detail: string }> {
    const projectName = String(input.project ?? input.name ?? "");
    const project = encodeURIComponent(projectName);
    let result: ApiResult;

    try {
        switch (name) {
            case "list_projects":
                result = await platform("/api/projects", { token });
                break;
            case "get_project":
                result = await platform(`/api/projects/${project}`, { token });
                break;
            case "create_project":
                result = await platform("/api/projects", { method: "POST", body: { name: input.name }, token });
                break;
            case "prepare_module": {
                result = await platform(`/api/projects/${project}/modules/${encodeURIComponent(moduleId)}`, {
                    method: "POST",
                    token,
                });
                // El contenido de los archivos no le sirve al agente y gasta tokens: solo los nombres
                const body = result.body as { files?: Record<string, string> } | null;
                if (result.ok && body?.files) {
                    result = { ...result, body: { ...body, files: Object.keys(body.files) } };
                }
                break;
            }
            case "create_resource":
                result = await platform(`/api/projects/${project}/resources`, {
                    method: "POST",
                    body: {
                        moduleId,
                        environment: input.environment,
                        name: input.name,
                        inputs: input.inputs ?? {},
                    },
                    token,
                });
                break;
            case "list_resources":
                result = await platform(`/api/projects/${project}/resources`, { token });
                break;
            default:
                return { ok: false, content: `Unknown tool ${name}`, detail: "unknown tool" };
        }
    } catch {
        return { ok: false, content: "The platform API is not reachable.", detail: "API not reachable" };
    }

    // Que un proyecto no exista es una respuesta válida para el agente, no un fallo
    const ok = result.ok || (name === "get_project" && result.status === 404);
    return {
        ok,
        content: JSON.stringify({ status: result.status, body: result.body }),
        detail: `${projectName ? projectName + " · " : ""}HTTP ${result.status}`,
    };
}

export async function runAgent(
    module: ModuleDetail,
    history: Anthropic.MessageParam[],
    message: string,
    token: string,
): Promise<{ history: Anthropic.MessageParam[]; reply: string; steps: Step[] }> {
    const messages: Anthropic.MessageParam[] = [...history, { role: "user", content: message }];
    const steps: Step[] = [];
    const texts: string[] = [];

    for (let turn = 0; turn < MAX_TURNS; turn++) {
        const response = await client.messages.create({
            model: MODEL,
            max_tokens: 1024,
            system: systemPrompt(module),
            tools: TOOLS,
            messages,
        });
        messages.push({ role: "assistant", content: response.content as Anthropic.ContentBlockParam[] });

        for (const block of response.content) {
            if (block.type === "text" && block.text.trim()) texts.push(block.text.trim());
        }
        if (response.stop_reason !== "tool_use") {
            return { history: messages, reply: texts.join("\n\n"), steps };
        }

        const results: Anthropic.ToolResultBlockParam[] = [];
        for (const block of response.content) {
            if (block.type !== "tool_use") continue;
            const outcome = await callTool(block.name, (block.input ?? {}) as Record<string, unknown>, module.id, token);
            steps.push({ tool: block.name, label: LABELS[block.name] ?? block.name, detail: outcome.detail, ok: outcome.ok });
            results.push({ type: "tool_result", tool_use_id: block.id, content: outcome.content, is_error: !outcome.ok });
        }
        messages.push({ role: "user", content: results });
    }

    texts.push("I stopped because this needed too many steps. Please try again.");
    return { history: messages, reply: texts.join("\n\n"), steps };
}