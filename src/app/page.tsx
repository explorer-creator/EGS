"use client";

import { useState } from "react";
import {
  Factory,
  Zap,
  Leaf,
  TrendingDown,
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Activity,
  BarChart2,
  Clock,
  Cpu,
} from "lucide-react";
import dynamic from "next/dynamic";
import { initialOrders, factoryStats, Order } from "@/lib/mockData";
import { formatNumber } from "@/lib/utils";
import OrderModal from "@/components/OrderModal";
import Toast from "@/components/Toast";

const EnergyChart = dynamic(() => import("@/components/EnergyChart"), {
  ssr: false,
});
const PriceChart = dynamic(() => import("@/components/PriceChart"), {
  ssr: false,
});

const priorityLabel: Record<string, string> = {
  high: "紧急",
  medium: "常规",
  low: "宽松",
};

const priorityStyle: Record<string, string> = {
  high: "bg-red-500/20 text-red-400 border-red-500/30",
  medium: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  low: "bg-slate-500/20 text-slate-400 border-slate-500/30",
};

interface ToastInfo {
  message: string;
}

export default function Dashboard() {
  const [orders, setOrders] = useState<Order[]>(initialOrders);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [toast, setToast] = useState<ToastInfo | null>(null);
  const [adoptedCount, setAdoptedCount] = useState(0);
  const [totalSaved, setTotalSaved] = useState(0);

  const pendingOrders = orders.filter((o) => o.status === "pending");

  const handleAdopt = (orderId: string, savedCost: number) => {
    setOrders((prev) =>
      prev.map((o) =>
        o.order_id === orderId ? { ...o, status: "adopted" as const } : o
      )
    );
    setSelectedOrder(null);
    setAdoptedCount((c) => c + 1);
    setTotalSaved((s) => s + savedCost);
    setToast({
      message: `已下发至车间执行，预计本次节约成本 ¥${formatNumber(savedCost)} 元`,
    });
  };

  const totalOriginalCost = pendingOrders.reduce(
    (sum, o) => sum + o.original_plan.est_cost_rmb,
    0
  );
  const totalOptimizedCost = pendingOrders.reduce(
    (sum, o) => sum + o.ai_optimized_plan.est_cost_rmb,
    0
  );
  const potentialSaving = totalOriginalCost - totalOptimizedCost;

  return (
    <div className="min-h-screen bg-[#060d1f]">
      {/* Toast */}
      {toast && (
        <Toast message={toast.message} onClose={() => setToast(null)} />
      )}

      {/* Modal */}
      {selectedOrder && (
        <OrderModal
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          onAdopt={handleAdopt}
        />
      )}

      {/* Top Nav */}
      <header className="border-b border-slate-800/60 bg-slate-950/80 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-screen-2xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-to-br from-green-500 to-emerald-600 rounded-xl flex items-center justify-center shadow-lg shadow-green-900/40">
              <Leaf size={18} className="text-white" />
            </div>
            <div>
              <h1 className="text-base font-bold text-white leading-tight">
                AI 绿能排产 & 碳排推演模拟器
              </h1>
              <p className="text-xs text-slate-500 leading-tight">
                Green Generate System · MVP v1.0
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {adoptedCount > 0 && (
              <div className="flex items-center gap-2 bg-green-500/10 border border-green-500/20 rounded-lg px-3 py-1.5">
                <CheckCircle2 size={14} className="text-green-400" />
                <span className="text-xs text-green-400 font-medium">
                  已优化 {adoptedCount} 单 · 累计节省 ¥{formatNumber(totalSaved)}
                </span>
              </div>
            )}
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
              实时监控中
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-screen-2xl mx-auto px-6 py-6 space-y-6">
        {/* KPI Stats Row */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={<Zap size={18} className="text-yellow-400" />}
            label="本月累计能耗"
            value={`${(factoryStats.monthly_total_energy_kwh / 1000).toFixed(1)}k`}
            unit="kWh"
            sub="较上月 +8.3%"
            subColor="text-red-400"
            iconBg="bg-yellow-500/10"
          />
          <StatCard
            icon={<Leaf size={18} className="text-green-400" />}
            label="本月累计碳排"
            value={`${(factoryStats.monthly_total_carbon_kg / 1000).toFixed(1)}k`}
            unit="kg CO₂"
            sub={`距目标还需减 ${formatNumber(factoryStats.carbon_reduction_target_kg)} kg`}
            subColor="text-orange-400"
            iconBg="bg-green-500/10"
          />
          <StatCard
            icon={<Activity size={18} className="text-blue-400" />}
            label="本月电费支出"
            value={`¥${(factoryStats.monthly_total_cost_rmb / 10000).toFixed(1)}万`}
            unit=""
            sub={`峰电占比 ${factoryStats.peak_ratio_percent}%`}
            subColor="text-red-400"
            iconBg="bg-blue-500/10"
          />
          <StatCard
            icon={<TrendingDown size={18} className="text-purple-400" />}
            label="AI 可优化节省"
            value={`¥${formatNumber(potentialSaving)}`}
            unit=""
            sub={`${pendingOrders.length} 单待优化`}
            subColor="text-green-400"
            iconBg="bg-purple-500/10"
            highlight
          />
        </div>

        {/* Main Grid */}
        <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
          {/* Left: Charts */}
          <div className="xl:col-span-3 space-y-5">
            {/* Energy Trend */}
            <div className="bg-slate-900/60 border border-slate-800/60 rounded-2xl p-5">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <BarChart2 size={16} className="text-green-400" />
                  <h2 className="text-sm font-semibold text-white">
                    工厂能耗 & 碳排趋势（近14日）
                  </h2>
                </div>
                <span className="text-xs text-slate-500 bg-slate-800 px-2 py-1 rounded">
                  实时数据
                </span>
              </div>
              <EnergyChart />
            </div>

            {/* Peak/Valley Price */}
            <div className="bg-slate-900/60 border border-slate-800/60 rounded-2xl p-5">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Clock size={16} className="text-blue-400" />
                  <h2 className="text-sm font-semibold text-white">
                    24小时峰谷电价分布
                  </h2>
                </div>
                <div className="flex items-center gap-3 text-xs">
                  <span className="flex items-center gap-1">
                    <span className="w-2 h-2 rounded-sm bg-red-500 inline-block" />
                    <span className="text-slate-400">峰电 ¥1.2</span>
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="w-2 h-2 rounded-sm bg-yellow-500 inline-block" />
                    <span className="text-slate-400">平电 ¥0.65</span>
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="w-2 h-2 rounded-sm bg-green-500 inline-block" />
                    <span className="text-slate-400">谷电 ¥0.3</span>
                  </span>
                </div>
              </div>
              <PriceChart />
            </div>
          </div>

          {/* Right: Order List */}
          <div className="xl:col-span-2">
            <div className="bg-slate-900/60 border border-slate-800/60 rounded-2xl p-5 h-full">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Cpu size={16} className="text-purple-400" />
                  <h2 className="text-sm font-semibold text-white">
                    待优化排产订单
                  </h2>
                  {pendingOrders.length > 0 && (
                    <span className="text-xs bg-red-500/20 text-red-400 border border-red-500/30 px-1.5 py-0.5 rounded-full font-bold">
                      {pendingOrders.length}
                    </span>
                  )}
                </div>
                <span className="text-xs text-slate-500">
                  点击订单进行 AI 分析
                </span>
              </div>

              {pendingOrders.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-16 text-center">
                  <CheckCircle2 size={40} className="text-green-500/40 mb-3" />
                  <p className="text-slate-400 text-sm font-medium">
                    所有订单均已优化
                  </p>
                  <p className="text-slate-600 text-xs mt-1">
                    累计节省 ¥{formatNumber(totalSaved)} 电费
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {pendingOrders.map((order) => {
                    const saving =
                      order.original_plan.est_cost_rmb -
                      order.ai_optimized_plan.est_cost_rmb;
                    return (
                      <button
                        key={order.order_id}
                        onClick={() => setSelectedOrder(order)}
                        className="w-full text-left bg-slate-800/50 hover:bg-slate-800 border border-slate-700/50 hover:border-green-500/30 rounded-xl p-4 transition-all group"
                      >
                        <div className="flex items-start justify-between mb-2">
                          <div>
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-xs font-mono text-slate-500">
                                {order.order_id}
                              </span>
                              <span
                                className={`text-xs border px-1.5 py-0.5 rounded font-medium ${priorityStyle[order.priority]}`}
                              >
                                {priorityLabel[order.priority]}
                              </span>
                            </div>
                            <p className="text-sm font-semibold text-white group-hover:text-green-300 transition-colors">
                              {order.product_name}
                            </p>
                            <p className="text-xs text-slate-500 mt-0.5">
                              {order.category}
                            </p>
                          </div>
                          <ChevronRight
                            size={16}
                            className="text-slate-600 group-hover:text-green-400 transition-colors mt-1 flex-shrink-0"
                          />
                        </div>
                        <div className="flex items-center justify-between mt-3 pt-3 border-t border-slate-700/50">
                          <div className="flex items-center gap-1">
                            <AlertCircle size={12} className="text-red-400" />
                            <span className="text-xs text-slate-400">
                              当前: {order.original_plan.schedule_time}
                            </span>
                          </div>
                          <div className="flex items-center gap-1">
                            <TrendingDown size={12} className="text-green-400" />
                            <span className="text-xs text-green-400 font-semibold">
                              可节省 ¥{formatNumber(saving)}
                            </span>
                          </div>
                        </div>
                        <div className="mt-2 flex items-center gap-1 text-xs">
                          <span className="w-1 h-1 rounded-full bg-blue-400 animate-pulse inline-block" />
                          <span className="text-blue-400/80">
                            点击启动 AI 绿能排产分析
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Adopted Orders */}
              {orders.filter((o) => o.status === "adopted").length > 0 && (
                <div className="mt-4 pt-4 border-t border-slate-800">
                  <p className="text-xs text-slate-600 mb-2">已优化执行</p>
                  <div className="space-y-2">
                    {orders
                      .filter((o) => o.status === "adopted")
                      .map((order) => (
                        <div
                          key={order.order_id}
                          className="flex items-center gap-2 bg-green-500/5 border border-green-500/10 rounded-lg px-3 py-2"
                        >
                          <CheckCircle2 size={12} className="text-green-500" />
                          <span className="text-xs text-slate-400 flex-1">
                            {order.product_name}
                          </span>
                          <span className="text-xs font-mono text-green-400">
                            {order.ai_optimized_plan.schedule_time}
                          </span>
                        </div>
                      ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  unit: string;
  sub: string;
  subColor: string;
  iconBg: string;
  highlight?: boolean;
}

function StatCard({
  icon,
  label,
  value,
  unit,
  sub,
  subColor,
  iconBg,
  highlight,
}: StatCardProps) {
  return (
    <div
      className={`rounded-2xl p-4 border transition-all ${
        highlight
          ? "bg-gradient-to-br from-purple-900/30 to-slate-900/60 border-purple-500/30"
          : "bg-slate-900/60 border-slate-800/60"
      }`}
    >
      <div className="flex items-start justify-between mb-3">
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${iconBg}`}>
          {icon}
        </div>
        {highlight && (
          <span className="text-xs bg-purple-500/20 text-purple-400 border border-purple-500/30 px-2 py-0.5 rounded-full">
            AI 推荐
          </span>
        )}
      </div>
      <p className="text-xs text-slate-500 mb-1">{label}</p>
      <div className="flex items-baseline gap-1">
        <span className="text-xl font-black text-white">{value}</span>
        {unit && <span className="text-xs text-slate-500">{unit}</span>}
      </div>
      <p className={`text-xs mt-1 ${subColor}`}>{sub}</p>
    </div>
  );
}
