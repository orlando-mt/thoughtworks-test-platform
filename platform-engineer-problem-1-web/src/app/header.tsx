import Link from "next/link";
import { cookies } from "next/headers";
import { signOut } from "@/app/actions";
import { USER_COOKIE } from "@/lib/platform";

export async function Header() {
    const user = (await cookies()).get(USER_COOKIE)?.value;
    return (
        <header className="border-b border-black/10 dark:border-white/15">
            <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
                <Link href="/" className="font-semibold tracking-tight">
                    Kordanix Platform
                </Link>
                {user && (
                    <form action={signOut} className="flex items-center gap-4 text-sm">
                        <span className="opacity-70">{user}</span>
                        <button className="underline underline-offset-4 hover:opacity-70">Sign out</button>
                    </form>
                )}
            </div>
        </header>
    );
}