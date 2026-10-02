import Link from "next/link";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Header } from "@/app/header";
import { platform, TOKEN_COOKIE, type ModuleSummary } from "@/lib/platform";

export default async function CatalogPage() {
  if (!(await cookies()).get(TOKEN_COOKIE)) {
    redirect("/login");
  }
  const response = await platform("/api/modules");
  if (response.status === 401) {
    redirect("/login");
  }
  const modules = response.ok ? (response.body as ModuleSummary[]) : [];

  return (
      <>
        <Header />
        <main className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">
          <h1 className="text-2xl font-semibold tracking-tight">Catalog</h1>
          <p className="mt-1 text-sm opacity-70">
            Published Terraform modules. Pick one and the assistant will guide you through the request.
          </p>

          {!response.ok && (
              <p role="alert" className="mt-6 text-sm text-red-600 dark:text-red-400">
                The catalog is not available right now (HTTP {response.status}).
              </p>
          )}

          <ul className="mt-8 grid gap-4 sm:grid-cols-2">
            {modules.map((module) => (
                <li key={module.id}>
                  <Link
                      href={`/chat/${module.id}`}
                      className="block h-full rounded-lg border border-black/10 p-5 transition hover:border-black/40 dark:border-white/15 dark:hover:border-white/50"
                  >
                    <div className="flex items-baseline justify-between gap-3">
                      <h2 className="font-medium">{module.displayName}</h2>
                      <span className="font-mono text-xs opacity-60">{module.version}</span>
                    </div>
                    <p className="mt-2 text-sm opacity-70">{module.description}</p>
                    <p className="mt-4 font-mono text-xs opacity-50">{module.id}</p>
                  </Link>
                </li>
            ))}
          </ul>
        </main>
      </>
  );
}