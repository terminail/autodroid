declare module '$app/navigation' {
	export function goto(
		href: string,
		opts?: {
			replaceState?: boolean;
			noScroll?: boolean;
			keepFocus?: boolean;
			invalidateAll?: boolean;
			state?: any;
		}
	): Promise<void>;
	export function invalidate(href: string): Promise<void>;
	export function invalidateAll(): Promise<void>;
	export function preloadCode(href: string): Promise<void>;
	export function preloadData(href: string): Promise<void>;
}