import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatNumber(num: number): string {
  return new Intl.NumberFormat("zh-CN").format(num);
}

export function calcSavingPercent(original: number, optimized: number): number {
  return Math.round(((original - optimized) / original) * 100);
}
