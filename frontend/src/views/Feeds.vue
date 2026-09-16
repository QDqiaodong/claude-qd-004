<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>饲料台账</span>
          <el-input v-model="query.keyword" placeholder="编号或名称" clearable size="small" style="width:170px" />
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:120px">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
          <el-button size="small" type="primary" @click="loadFeeds">查询</el-button>
          <el-button size="small" type="success" @click="openFeed()">新增饲料</el-button>
          <el-button size="small" type="warning" @click="openIssue('领用')">领料</el-button>
          <el-button size="small" @click="openIssue('退料')">退料</el-button>
        </div>
      </template>
      <el-table :data="feeds" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="饲料编号" width="120" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.stock <= row.warnStock ? '#e6a23c' : '' }">
              {{ row.stock }}{{ row.stock <= row.warnStock ? ' ↓' : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="warnStock" label="预警线" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFeed(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>领用流水</span>
          <el-select v-model="filterBarnId" placeholder="只看某个牛舍" clearable size="small" style="width:180px">
            <el-option v-for="b in barns" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
          <el-button size="small" type="primary" @click="loadIssues">查询</el-button>
          <span style="margin-left:auto;color:#909399">共 {{ issues.length }} 条</span>
        </div>
      </template>
      <el-table :data="issues" border stripe size="small" v-loading="loadingIssues">
        <el-table-column label="牛舍" width="150">
          <template #default="{ row }">{{ barnName(row.barnId) }}</template>
        </el-table-column>
        <el-table-column label="饲料" min-width="150">
          <template #default="{ row }">{{ feedName(row.feedId) }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.kind === '领用' ? 'danger' : 'success'" size="small">{{ row.kind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="经手人" width="110" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="feedVisible" :title="feedForm.id ? '调整饲料' : '新增饲料'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="饲料编号">
          <el-input v-model="feedForm.code" :disabled="!!feedForm.id" placeholder="如 FD-1006" />
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="feedForm.name" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="feedForm.unit" style="width:100%">
            <el-option v-for="u in ['公斤', '袋', '捆']" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存"><el-input-number v-model="feedForm.stock" :min="0" /></el-form-item>
        <el-form-item label="预警线"><el-input-number v-model="feedForm.warnStock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="feedForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeed">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" :title="issueForm.kind === '领用' ? '领料' : '退料'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="牛舍">
          <el-select v-model="issueForm.barnId" placeholder="只列在用的牛舍" style="width:100%">
            <el-option v-for="b in usableBarns" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="饲料">
          <el-select v-model="issueForm.feedId" placeholder="只列在用的饲料" style="width:100%">
            <el-option
              v-for="f in usableFeeds"
              :key="f.id"
              :label="`${f.name}（${f.code} · 库存 ${f.stock} ${f.unit}）`"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="issueForm.qty" :min="1" /></el-form-item>
        <el-form-item label="经手人"><el-input v-model="issueForm.operator" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIssue">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { barnApi, feedApi, issueApi } from '../api'

const feeds = ref([])
const barns = ref([])
const issues = ref([])
const loading = ref(false)
const loadingIssues = ref(false)
const query = reactive({ keyword: '', status: '' })
const filterBarnId = ref(null)

const feedVisible = ref(false)
const feedForm = reactive({ id: null, code: '', name: '', unit: '公斤', stock: 0, warnStock: 0, status: '在用' })
const issueVisible = ref(false)
const issueForm = reactive({ barnId: null, feedId: null, qty: 1, kind: '领用', operator: '' })

const usableFeeds = computed(() => feeds.value.filter((f) => f.status === '在用'))
const usableBarns = computed(() => barns.value.filter((b) => b.status === '在用'))

const barnName = (id) => (id ? barns.value.find((b) => b.id === id)?.name || `#${id}` : '—')
const feedName = (id) => (id ? feeds.value.find((f) => f.id === id)?.name || `#${id}` : '—')

const loadFeeds = async () => {
  loading.value = true
  try {
    feeds.value = await feedApi.list({
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const loadIssues = async () => {
  loadingIssues.value = true
  try {
    issues.value = await issueApi.list(filterBarnId.value || undefined)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loadingIssues.value = false
  }
}

const openFeed = (row) => {
  if (row) {
    Object.assign(feedForm, row)
  } else {
    Object.assign(feedForm, { id: null, code: '', name: '', unit: '公斤', stock: 0, warnStock: 0, status: '在用' })
  }
  feedVisible.value = true
}

const submitFeed = async () => {
  try {
    if (feedForm.id) {
      await feedApi.update(feedForm.id, {
        name: feedForm.name,
        unit: feedForm.unit,
        stock: feedForm.stock,
        warnStock: feedForm.warnStock,
        status: feedForm.status
      })
    } else {
      await feedApi.create({ ...feedForm })
    }
    ElMessage.success('已保存')
    feedVisible.value = false
    await loadFeeds()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openIssue = (kind) => {
  Object.assign(issueForm, { barnId: null, feedId: null, qty: 1, kind, operator: '' })
  issueVisible.value = true
}

const submitIssue = async () => {
  try {
    await issueApi.create({ ...issueForm })
    ElMessage.success(issueForm.kind === '领用' ? '已领料' : '已退料')
    issueVisible.value = false
    await loadFeeds()
    await loadIssues()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    barns.value = await barnApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadFeeds()
  await loadIssues()
})
</script>
