import Link from "next/link";
import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { Header } from "@/app/header";
import { Chat } from "./chat";
import { platform, TOKEN_COOKIE, type ModuleDetail } from "@/lib/platform";

export default async function ChatPage({ params }: { params: Promise<{ moduleId: string }> }) {
    const { moduleId } = await params;
    if (!(await cookies()).get(TOKEN_COOKIE)) {
        redirect("/login");
    }
    const response = await platform(`/api/modules/${encodeURIComponent(moduleId)}`);
    if (response.status === 401) {
        redirect("/login");
    }
    if (!response.ok) {
        notFound();
    }
    const module = response.body as ModuleDetail;

    return (
        <>
            <Header />
            <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col px-6 py-8">
                <Link href="/" className="text-sm underline underline-offset-4 opacity-70 hover:opacity-100">
                    ← Catalog
                </Link>
                <div className="mt-4 flex items-baseline justify-between gap-3">
                    <h1 className="text-xl font-semibold tracking-tight">{module.displayName}</h1>
                    <span className="font-mono text-xs opacity-60">
            {module.id} · {module.version}
          </span>
                </div>
                <p className="mt-1 text-sm opacity-70">{module.description}</p>
                <Chat moduleId={module.id} displayName={module.displayName} />
            </main>
        </>
    );
}