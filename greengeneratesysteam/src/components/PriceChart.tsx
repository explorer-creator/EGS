"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { timeSlots } from "@/lib/mockData";

const typeColor: Record<string, string> = {
  peak: "#ef4444",
  shoulder: "#f59e0b",
  valley: "#22c55e",
};

const CustomTooltip = ({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: Array<{ value: number; payload: { type: string; carbon_factor: number } }>;
  label?: string;
}) => {
  if (active && payload && payload.length) {
    const type = payload[0].payload.type;
    const typeLabel = type === "peak" ? "峰电" : type === "valley" ? "谷电" : "平电";
    const typeColorVal = typeColor[type];
    return (
      <div className="bg-slate-900 border border-slate-700 rounded-lg p-3 shadow-xl text-xs">
        <p className="text-slate-300 font-semibold mb-1">{label}</p>
        <p style={{ color: typeColorVal }} className="font-bold mb-1">
          {typeLabel}段
        </p>
        <p className="text-slate-400">
          电价: <span className="text-white">¥{payload[0].value}/kWh</span>
        </p>
        <p className="text-slate-400">
          碳因子:{" "}
          <span className="text-white">
            {payload[0].payload.carbon_factor} kg/kWh
          </span>
        </p>
      </div>
    );
  }
  return null;
};

export default function PriceChart() {
  return (
    <div className="w-full h-48">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={timeSlots}
          margin={{ top: 5, right: 5, left: -20, bottom: 5 }}
          barSize={10}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
          <XAxis
            dataKey="hour"
            tick={{ fill: "#64748b", fontSize: 9 }}
            axisLine={{ stroke: "#1e293b" }}
            tickLine={false}
            interval={3}
          />
          <YAxis
            tick={{ fill: "#64748b", fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            domain={[0, 1.4]}
          />
          <Tooltip content={<CustomTooltip />} />
          <Bar dataKey="price_per_kwh" radius={[2, 2, 0, 0]}>
            {timeSlots.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={typeColor[entry.type]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
