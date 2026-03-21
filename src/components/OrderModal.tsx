"use client";

import { useState } from "react";
import { X, Zap, Leaf, Clock, DollarSign, Brain, CheckCircle, AlertTriangle } from "lucide-react";
import { Order } from "@/lib/mockData";
import { calcSavingPercent, formatNumber } from "@/lib/utils";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface OrderModalProps {
  order: Order;
  onClose: () => void;
  onAdopt: (orderId: string, savedCost: number) => void;
}

export default function OrderModal({ order, onClose, onAdopt }: OrderModalProps) {
  const [adopting, setAdopting] = useState(false);

  const costSavePct = calcSavingPercent(
    order.original_plan.est_cost_rmb,
    order.ai_optimized_plan.est_cost_rmb
  );
  const carbonSavePct = calcSavingPercent(
    order.original_plan.est_carbon_kg,
    order.ai_optimized_plan.est_carbon_kg
  );
  const savedCost =
    order.original_plan.est_cost_rmb - order.ai_optimized_plan.est_cost_rmb;
  const savedCarbon =
    order.original_plan.est_carbon_kg - order.ai_optimized_plan.est_carbon_kg;

  const compareData = [
    {
      name: "电费 (¥)",
      常规方案: order.original_plan.est_cost_rmb,
      AI优化方案: order.ai_optimized_plan.est_cost_rmb,
    },
    {
      name: "碳排 (kg)",
      常规方案: order.original_plan.est_carbon_kg,
      AI优化方案: order.ai_optimized_plan.est_carbon_kg,
    },
    {
      name: "能耗 (kWh)",
      常规方案: order.original_plan.est_energy_kwh,
      AI优化方案: order.ai_optimized_plan.est_energy_kwh,
    },
  ];

  const handleAdopt = () => {
    setAdopting(true);
    setTimeout(() => {
      onAdopt(order.order_id, savedCost);
    }, 800);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative w-full max-w-4xl mx-4 bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl animate-fade-in max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between p-6 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-mono bg-slate-800 text-slate-400 px-2 py-0.5 rounded">
                {order.order_id}
              </span>
              <span className="text-xs bg-blue-500/20 text-blue-400 border border-blue-500/30 px-2 py-0.5 rounded">
                {order.category}
              </span>
            </div>
            <h2 className="text-xl font-bold text-white">{order.product_name}</h2>
            <p className="text-sm text-slate-400 mt-1">AI 绿能排产优化分析报告</p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-500 hover:text-white transition-colors p-1 rounded-lg hover:bg-slate-800"
          >
            <X size={20} />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Side-by-side plan comparison */}
          <div className="grid grid-cols-2 gap-4">
            {/* Original Plan */}
            <div className="bg-red-500/5 border border-red-500/20 rounded-xl p-4">
              <div className="flex items-center gap-2 mb-4">
                <AlertTriangle size={16} className="text-red-400" />
                <span className="text-sm font-semibold text-red-400">常规方案（当前）</span>
              </div>
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <Clock size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">排产时段</span>
                  <span className="ml-auto text-sm font-semibold text-red-300">
                    {order.original_plan.schedule_time}
                  </span>
                </div>
                <div className="h-px bg-slate-800" />
                <div className="flex items-center gap-2">
                  <DollarSign size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">预估电费</span>
                  <span className="ml-auto text-lg font-bold text-red-400">
                    ¥{formatNumber(order.original_plan.est_cost_rmb)}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Leaf size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">碳排放量</span>
                  <span className="ml-auto text-base font-bold text-red-300">
                    {formatNumber(order.original_plan.est_carbon_kg)} kg
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Zap size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">能耗总量</span>
                  <span className="ml-auto text-sm font-semibold text-slate-300">
                    {formatNumber(order.original_plan.est_energy_kwh)} kWh
                  </span>
                </div>
                <div className="mt-2 bg-red-500/10 rounded-lg p-2 text-xs text-red-400 text-center">
                  峰电时段 · 电价 ¥1.2/kWh · 高碳因子
                </div>
              </div>
            </div>

            {/* AI Optimized Plan */}
            <div className="bg-green-500/5 border border-green-500/30 rounded-xl p-4 glow-green">
              <div className="flex items-center gap-2 mb-4">
                <CheckCircle size={16} className="text-green-400" />
                <span className="text-sm font-semibold text-green-400">AI 优化方案（推荐）</span>
              </div>
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <Clock size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">排产时段</span>
                  <span className="ml-auto text-sm font-semibold text-green-300">
                    {order.ai_optimized_plan.schedule_time}
                  </span>
                </div>
                <div className="h-px bg-slate-800" />
                <div className="flex items-center gap-2">
                  <DollarSign size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">预估电费</span>
                  <div className="ml-auto flex items-center gap-1">
                    <span className="text-lg font-bold text-green-400">
                      ¥{formatNumber(order.ai_optimized_plan.est_cost_rmb)}
                    </span>
                    <span className="text-xs bg-green-500/20 text-green-400 px-1.5 py-0.5 rounded font-bold">
                      -{costSavePct}%
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Leaf size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">碳排放量</span>
                  <div className="ml-auto flex items-center gap-1">
                    <span className="text-base font-bold text-green-300">
                      {formatNumber(order.ai_optimized_plan.est_carbon_kg)} kg
                    </span>
                    <span className="text-xs bg-green-500/20 text-green-400 px-1.5 py-0.5 rounded font-bold">
                      -{carbonSavePct}%
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Zap size={14} className="text-slate-500" />
                  <span className="text-xs text-slate-400">能耗总量</span>
                  <span className="ml-auto text-sm font-semibold text-slate-300">
                    {formatNumber(order.ai_optimized_plan.est_energy_kwh)} kWh
                  </span>
                </div>
                <div className="mt-2 bg-green-500/10 rounded-lg p-2 text-xs text-green-400 text-center">
                  谷电时段 · 电价 ¥0.3/kWh · 低碳因子
                </div>
              </div>
            </div>
          </div>

          {/* Savings Summary */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-gradient-to-br from-yellow-500/10 to-orange-500/10 border border-yellow-500/20 rounded-xl p-4 text-center">
              <p className="text-xs text-slate-400 mb-1">节省电费</p>
              <p className="text-2xl font-black text-yellow-400">
                ¥{formatNumber(savedCost)}
              </p>
              <p className="text-xs text-yellow-500/80 mt-1">降幅 {costSavePct}%</p>
            </div>
            <div className="bg-gradient-to-br from-green-500/10 to-emerald-500/10 border border-green-500/20 rounded-xl p-4 text-center">
              <p className="text-xs text-slate-400 mb-1">减少碳排</p>
              <p className="text-2xl font-black text-green-400">
                {formatNumber(savedCarbon)} kg
              </p>
              <p className="text-xs text-green-500/80 mt-1">降幅 {carbonSavePct}%</p>
            </div>
          </div>

          {/* Comparison Bar Chart */}
          <div>
            <h3 className="text-sm font-semibold text-slate-300 mb-3">方案数据对比</h3>
            <div className="h-44 bg-slate-950/50 rounded-xl p-3">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={compareData} barGap={4}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis
                    dataKey="name"
                    tick={{ fill: "#64748b", fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tick={{ fill: "#64748b", fontSize: 10 }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(v: number) =>
                      v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v)
                    }
                  />
                  <Tooltip
                    contentStyle={{
                      background: "#0f172a",
                      border: "1px solid #1e293b",
                      borderRadius: "8px",
                      fontSize: "12px",
                    }}
                    formatter={(value: number) => value.toLocaleString("zh-CN")}
                  />
                  <Legend
                    wrapperStyle={{ fontSize: "12px" }}
                    formatter={(value) => (
                      <span style={{ color: "#94a3b8" }}>{value}</span>
                    )}
                  />
                  <Bar dataKey="常规方案" fill="#ef4444" radius={[3, 3, 0, 0]} opacity={0.8} />
                  <Bar dataKey="AI优化方案" fill="#22c55e" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* AI Reasoning */}
          {order.ai_optimized_plan.ai_reasoning && (
            <div className="bg-gradient-to-r from-blue-500/5 via-purple-500/5 to-green-500/5 border border-blue-500/20 rounded-xl p-4">
              <div className="flex items-center gap-2 mb-3">
                <Brain size={16} className="text-blue-400" />
                <span className="text-sm font-semibold text-blue-400">AI 分析推理</span>
                <span className="text-xs bg-purple-500/20 text-purple-400 border border-purple-500/30 px-2 py-0.5 rounded ml-auto">
                  绿能优化引擎 v2.1
                </span>
              </div>
              <p className="text-sm text-slate-300 leading-relaxed">
                {order.ai_optimized_plan.ai_reasoning}
              </p>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-3 pt-2">
            <button
              onClick={onClose}
              className="flex-1 py-3 rounded-xl border border-slate-700 text-slate-400 hover:text-white hover:border-slate-600 transition-all text-sm font-medium"
            >
              稍后决定
            </button>
            <button
              onClick={handleAdopt}
              disabled={adopting}
              className="flex-[2] py-3 rounded-xl bg-gradient-to-r from-green-600 to-emerald-500 hover:from-green-500 hover:to-emerald-400 text-white font-bold text-sm transition-all shadow-lg shadow-green-900/30 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {adopting ? (
                <>
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  下发执行中...
                </>
              ) : (
                <>
                  <CheckCircle size={16} />
                  采纳优化方案 · 节省 ¥{formatNumber(savedCost)}
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
