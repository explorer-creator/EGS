import { haversineKm } from './geo'

/**
 * 城际货运运时数学模型（参考估算）
 *
 * D0 = 两市球面直线距离（Haversine, km）
 * 不同交通方式采用：等效路网距离 D_eff = D0 × 绕行系数
 * 在途时间 T_transit = D_eff / v
 * 总时间 T = T_transit + T_handling（装卸、集散、安检等固定环节，小时）
 *
 * 参数参考量级：公路干线、铁路货运、航空货运的典型速度与环节时间，非精确调度。
 */
const MODES = {
  road: {
    key: 'road',
    label: '公路干线货运',
    /** 相对直线距离的绕行系数（高速/国道综合） */
    detour: 1.38,
    /** 平均有效速度 km/h（含休息法规约束的等效） */
    speedKmh: 68,
    /** 起终点装卸与园区集散（小时） */
    handlingH: 4
  },
  rail: {
    key: 'rail',
    label: '铁路货运',
    detour: 1.12,
    speedKmh: 95,
    handlingH: 8
  },
  air: {
    key: 'air',
    label: '航空货运',
    /** 空运按大圆航线，绕行系数近似 1 */
    detour: 1.02,
    /** 巡航等效地速 */
    speedKmh: 720,
    /** 两端货站、安检、装机（小时） */
    handlingH: 14
  }
}

/**
 * @param {{lat:number,lng:number}} origin
 * @param {{lat:number,lng:number}} dest
 * @param {'road'|'rail'|'air'} modeKey
 */
export function estimateDelivery(origin, dest, modeKey) {
  const mode = MODES[modeKey] || MODES.road
  const a = [origin.lat, origin.lng]
  const b = [dest.lat, dest.lng]
  const d0 = haversineKm(a, b)
  const dEff = d0 * mode.detour
  const transitH = dEff / mode.speedKmh
  const totalH = transitH + mode.handlingH

  return {
    straightKm: d0,
    effectiveKm: dEff,
    mode: mode.label,
    transitHours: transitH,
    handlingHours: mode.handlingH,
    totalHours: totalH,
    detour: mode.detour,
    speedKmh: mode.speedKmh,
    formula: `T = (${d0.toFixed(0)}×${mode.detour})/${mode.speedKmh} + ${mode.handlingH} ≈ ${totalH.toFixed(1)} h`
  }
}

export function formatDurationHours(h) {
  if (h == null || Number.isNaN(h)) return '-'
  if (h < 24) return `${h.toFixed(1)} 小时`
  const d = Math.floor(h / 24)
  const hr = h - d * 24
  return `${d} 天 ${hr.toFixed(1)} 小时（约 ${h.toFixed(1)} 小时）`
}

export { MODES }
