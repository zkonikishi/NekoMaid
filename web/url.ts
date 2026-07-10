const localParams = new URLSearchParams(location.search)
const localToken = localParams.get('token')
let url = localToken
  ? `${location.origin}/?token=${encodeURIComponent(localToken)}`
  : decodeURIComponent(location.search.slice(1))
let parsed: URL | null = null

if (!url.startsWith('http://') && !url.startsWith('https://')) url = location.protocol + '//' + url

// eslint-disable-next-line no-new
try { parsed = new URL(url) } catch { url = '' }

export const pathname = localToken ? '/' : parsed?.pathname
export const origin = parsed?.origin
export const address = parsed ? parsed.origin + parsed.pathname : null
export const token = localToken || parsed?.search?.slice(1)?.replace('token=', '')
export default token ? url : null
