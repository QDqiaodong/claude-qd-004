import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const barnApi = {
  list: (params) => http.get('/barns', { params }),
  create: (data) => http.post('/barns', data),
  update: (id, data) => http.put(`/barns/${id}`, data)
}

export const stallApi = {
  list: (params) => http.get('/stalls', { params }),
  create: (data) => http.post('/stalls', data),
  update: (id, data) => http.put(`/stalls/${id}`, data)
}

export const cowApi = {
  list: (params) => http.get('/cows', { params }),
  create: (data) => http.post('/cows', data),
  update: (id, data) => http.put(`/cows/${id}`, data),
  // 登记淘汰：后端在同一笔事务里置「已淘汰/离栏」并摘掉牛舍名额
  cull: (id) => http.post(`/cows/${id}/cull`)
}

export const shiftApi = {
  list: (params) => http.get('/shifts', { params }),
  open: (data) => http.post('/shifts', data),
  advance: (id, action, milkKg) =>
    http.post(`/shifts/${id}/advance`, null, { params: { action, milkKg } })
}

export const feedApi = {
  list: (params) => http.get('/feeds', { params }),
  create: (data) => http.post('/feeds', data),
  update: (id, data) => http.put(`/feeds/${id}`, data)
}

export const issueApi = {
  list: (barnId) => http.get('/feed-issues', { params: barnId ? { barnId } : {} }),
  create: (data) => http.post('/feed-issues', data)
}

export const milkTestApi = {
  list: (shiftId) => http.get('/milk-tests', { params: shiftId ? { shiftId } : {} }),
  sampling: (data) => http.post('/milk-tests/samplings', data),
  disposition: (data) => http.post('/milk-tests/dispositions', data)
}

export default http
