/**
 * 设施点：厂房、仓库、物料集散（上海及周边示例坐标）
 * 实际项目可从后端 / 配置加载
 */
export const DEFAULT_CENTER = [31.2304, 121.4737] // 上海人民广场 [lat,lng]

export const FACILITIES = [
  {
    id: 'f1',
    name: '华东一厂（总装）',
    type: '厂房',
    lat: 31.245,
    lng: 121.42,
    remark: '行星减速器总装'
  },
  {
    id: 'f2',
    name: '精密加工中心',
    type: '厂房',
    lat: 31.22,
    lng: 121.5,
    remark: '车铣磨与检测'
  },
  {
    id: 'w1',
    name: '中央成品仓',
    type: '仓库',
    lat: 31.25,
    lng: 121.45,
    remark: 'WMS 管理'
  },
  {
    id: 'w2',
    name: '原料暂存库',
    type: '仓库',
    lat: 31.18,
    lng: 121.38,
    remark: '来料 IQC'
  },
  {
    id: 'm1',
    name: '物料集散点 A',
    type: '物料',
    lat: 31.28,
    lng: 121.52,
    remark: '干线到拨'
  },
  {
    id: 'm2',
    name: '物料集散点 B',
    type: '物料',
    lat: 31.15,
    lng: 121.48,
    remark: '区域配送'
  }
]

export const TYPE_COLORS = {
  厂房: '#E6A23C',
  仓库: '#409EFF',
  物料: '#67C23A'
}
