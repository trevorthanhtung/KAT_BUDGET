const VCB_RATE_URL = 'https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx'
const CACHE_TTL_MS = 5 * 60 * 1000

let cachedXml = ''
let cachedAt = 0

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

  if (req.method === 'OPTIONS') {
    res.status(204).end()
    return
  }

  if (req.method !== 'GET') {
    res.status(405).json({ error: 'Method not allowed' })
    return
  }

  try {
    const now = Date.now()
    const hasFreshCache = cachedXml && now - cachedAt < CACHE_TTL_MS

    if (!hasFreshCache) {
      const response = await fetch(VCB_RATE_URL)
      if (!response.ok) {
        throw new Error(`Vietcombank returned ${response.status}`)
      }

      cachedXml = await response.text()
      cachedAt = now
    }

    res.setHeader('Content-Type', 'application/xml; charset=utf-8')
    res.setHeader('Cache-Control', 's-maxage=300, stale-while-revalidate=300')
    res.status(200).send(cachedXml)
  } catch {
    res.status(500).json({ error: 'Lỗi khi tải tỷ giá Vietcombank' })
  }
}
