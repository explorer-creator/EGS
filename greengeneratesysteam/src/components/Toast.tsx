"use client";

import { useEffect } from "react";
import { CheckCircle, X } from "lucide-react";

interface ToastProps {
  message: string;
  onClose: () => void;
  duration?: number;
}

export default function Toast({ message, onClose, duration = 4000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [onClose, duration]);

  return (
    <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[100] animate-slide-in-top">
      <div className="flex items-center gap-3 bg-gradient-to-r from-green-900/95 to-emerald-900/95 border border-green-500/40 rounded-xl px-5 py-3.5 shadow-2xl shadow-green-900/50 backdrop-blur-sm min-w-[320px] max-w-[520px]">
        <div className="flex-shrink-0 w-8 h-8 bg-green-500/20 rounded-full flex items-center justify-center">
          <CheckCircle size={16} className="text-green-400" />
        </div>
        <div className="flex-1">
          <p className="text-sm font-semibold text-green-300">排产计划已更新</p>
          <p className="text-xs text-green-400/80 mt-0.5">{message}</p>
        </div>
        <button
          onClick={onClose}
          className="flex-shrink-0 text-green-500/60 hover:text-green-300 transition-colors ml-2"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
}
