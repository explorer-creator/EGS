/**
 * 网格最短路径（与无权重网格上 A* 曼哈顿启发式等价，结果一致）
 */
export function astarPath(grid, start, goal) {
  const rows = grid.length
  const cols = grid[0]?.length || 0
  if (!rows || !cols) return null
  const [sr, sc] = start
  const [gr, gc] = goal
  if (grid[sr]?.[sc] === 1 || grid[gr]?.[gc] === 1) return null

  const key = (r, c) => `${r},${c}`
  const h = (r, c) => Math.abs(r - gr) + Math.abs(c - gc)
  const open = new Set([key(sr, sc)])
  const gScore = new Map([[key(sr, sc), 0]])
  const fScore = new Map([[key(sr, sc), h(sr, sc)]])
  const came = new Map()
  const neighbors = [[-1, 0], [1, 0], [0, -1], [0, 1]]

  while (open.size) {
    let current = null
    let bestF = Infinity
    for (const k of open) {
      const f = fScore.get(k) ?? Infinity
      if (f < bestF) {
        bestF = f
        current = k
      }
    }
    const [cr, cc] = current.split(',').map(Number)
    open.delete(current)

    if (cr === gr && cc === gc) {
      const path = []
      let cur = current
      while (cur) {
        path.push(cur.split(',').map(Number))
        cur = came.get(cur)
      }
      path.reverse()
      return { path, length: path.length - 1 }
    }

    for (const [dr, dc] of neighbors) {
      const nr = cr + dr
      const nc = cc + dc
      if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
      if (grid[nr][nc] === 1) continue
      const nk = key(nr, nc)
      const tentative = (gScore.get(current) || 0) + 1
      if (tentative < (gScore.get(nk) ?? Infinity)) {
        came.set(nk, current)
        gScore.set(nk, tentative)
        fScore.set(nk, tentative + h(nr, nc))
        open.add(nk)
      }
    }
  }
  return null
}

/**
 * 多停靠点：最近邻顺序（简化车辆路径）
 */
export function nearestNeighborOrder(start, stops) {
  if (!stops.length) return [start]
  const remaining = stops.map(s => [...s])
  const order = [[...start]]
  let cur = [...start]
  while (remaining.length) {
    let bestIdx = 0
    let bestD = Infinity
    for (let i = 0; i < remaining.length; i++) {
      const d = Math.abs(cur[0] - remaining[i][0]) + Math.abs(cur[1] - remaining[i][1])
      if (d < bestD) {
        bestD = d
        bestIdx = i
      }
    }
    const next = remaining.splice(bestIdx, 1)[0]
    order.push([...next])
    cur = [...next]
  }
  return order
}
