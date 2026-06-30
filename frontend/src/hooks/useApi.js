import { useState, useEffect, useCallback } from 'react'

/**
 * Hook per chiamate API con gestione automatica di loading/error.
 * @param {Function} apiFn - funzione che ritorna una Promise (es. () => api.get('/endpoint'))
 * @param {Array} deps - dipendenze per il re-fetch
 * @param {Object} options - { immediate: bool (default true), initialData }
 */
function useApi(apiFn, deps = [], { immediate = true, initialData = null } = {}) {
  const [data, setData] = useState(initialData)
  const [loading, setLoading] = useState(immediate)
  const [error, setError] = useState(null)

  const execute = useCallback(async (...args) => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFn(...args)
      const result = res?.data ?? res
      setData(result)
      return result
    } catch (err) {
      const msg = err?.response?.data?.error || err?.message || 'Errore sconosciuto'
      setError(msg)
      throw err
    } finally {
      setLoading(false)
    }
  }, deps) // eslint-disable-line

  useEffect(() => {
    if (immediate) execute()
  }, [execute]) // eslint-disable-line

  return { data, loading, error, refetch: execute }
}

export default useApi
