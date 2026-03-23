/** 地球半径 km */
const R = 6371

/**
 * Haversine 球面距离（km）
 * @param {[number,number]} a [lat, lng]
 * @param {[number,number]} b [lat, lng]
 */
export function haversineKm(a, b) {
  const [lat1, lon1] = a
  const [lat2, lon2] = b
  const toRad = (d) => (d * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const x =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(x)))
}

export function formatDistance(km) {
  if (km == null || Number.isNaN(km)) return '-'
  if (km < 1) return `${Math.round(km * 1000)} m`
  return `${km.toFixed(2)} km`
}

/** 高德格式 "lng,lat" -> [lat, lng] for Leaflet */
export function amapToLatLng(s) {
  const [lng, lat] = s.split(',').map(Number)
  return [lat, lng]
}

/** [lat,lng] -> "lng,lat" 高德 */
export function latLngToAmap(lat, lng) {
  return `${lng},${lat}`
}
