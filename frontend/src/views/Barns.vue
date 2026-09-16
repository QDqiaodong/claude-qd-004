<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>牛舍</span>
          <el-button size="small" type="success" @click="openBarn()">新增牛舍</el-button>
          <span style="margin-left:auto;color:#909399">
            在用 {{ barns.filter(b => b.status === '在用').length }} 个 / 共 {{ barns.length }} 个
          </span>
        </div>
      </template>
      <el-table :data="barns" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="牛舍编号" width="110" />
        <el-table-column prop="name" label="牛舍名称" min-width="150" />
        <el-table-column prop="kind" label="类型" width="100" />
        <el-table-column prop="capacity" label="可容纳" width="100" />
        <el-table-column label="在栏头数" width="110">
          <template #default="{ row }">{{ cows.filter(c => c.barnId === row.id && c.status === '在栏').length }}</template>
        </el-table-column>
        <el-table-column label="名下挤奶位" width="120">
          <template #default="{ row }">{{ stalls.filter(s => s.barnId === row.id).length }} 个</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBarn(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>挤奶位台账</span>
          <el-select v-model="query.barnId" placeholder="按牛舍" clearable size="small" style="width:170px">
            <el-option v-for="b in barns" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:130px">
            <el-option label="可用" value="可用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修" value="维修" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="编号或名称" clearable size="small" style="width:170px" />
          <el-button size="small" type="primary" @click="loadStalls">查询</el-button>
          <el-button size="small" type="success" @click="openStall()">新增挤奶位</el-button>
        </div>
      </template>
      <el-table :data="stalls" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="归属牛舍" width="160">
          <template #default="{ row }">{{ barnName(row.barnId) }}</template>
        </el-table-column>
        <el-table-column prop="stallType" label="型式" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '可用' ? 'success' : row.status === '维修' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStall(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="barnVisible" :title="barnForm.id ? '调整牛舍' : '新增牛舍'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="牛舍编号">
          <el-input v-model="barnForm.code" :disabled="!!barnForm.id" placeholder="如 BN-06" />
        </el-form-item>
        <el-form-item label="牛舍名称"><el-input v-model="barnForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="barnForm.kind" style="width:100%">
            <el-option v-for="k in ['产奶舍', '犊牛舍', '干奶舍', '隔离舍']" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="可容纳头数">
          <el-input-number v-model="barnForm.capacity" :min="0" :step="5" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="barnForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="barnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBarn">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stallVisible" :title="stallForm.id ? '调整挤奶位' : '新增挤奶位'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="编号">
          <el-input v-model="stallForm.code" :disabled="!!stallForm.id" placeholder="如 MS-06" />
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="stallForm.name" /></el-form-item>
        <el-form-item label="归属牛舍">
          <el-select v-model="stallForm.barnId" clearable placeholder="可先不归" style="width:100%">
            <el-option v-for="b in barns" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="型式">
          <el-select v-model="stallForm.stallType" style="width:100%">
            <el-option label="并列式" value="并列式" />
            <el-option label="转盘式" value="转盘式" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="stallForm.status" style="width:100%">
            <el-option label="可用" value="可用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修" value="维修" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stallVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStall">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { barnApi, cowApi, stallApi } from '../api'

const barns = ref([])
const stalls = ref([])
const cows = ref([])
const loading = ref(false)
const query = reactive({ barnId: null, status: '', keyword: '' })

const barnVisible = ref(false)
const barnForm = reactive({ id: null, code: '', name: '', kind: '产奶舍', capacity: 0, status: '在用' })
const stallVisible = ref(false)
const stallForm = reactive({ id: null, code: '', name: '', barnId: null, stallType: '并列式', status: '可用' })

const barnName = (id) => (id ? barns.value.find((b) => b.id === id)?.name || '未归舍' : '未归舍')

const loadBarns = async () => {
  barns.value = await barnApi.list({})
}

const loadStalls = async () => {
  loading.value = true
  try {
    stalls.value = await stallApi.list({
      barnId: query.barnId || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openBarn = (row) => {
  if (row) {
    Object.assign(barnForm, row)
  } else {
    Object.assign(barnForm, { id: null, code: '', name: '', kind: '产奶舍', capacity: 0, status: '在用' })
  }
  barnVisible.value = true
}

const submitBarn = async () => {
  try {
    if (barnForm.id) {
      await barnApi.update(barnForm.id, {
        name: barnForm.name,
        kind: barnForm.kind,
        capacity: barnForm.capacity,
        status: barnForm.status
      })
    } else {
      await barnApi.create({ ...barnForm })
    }
    ElMessage.success('已保存')
    barnVisible.value = false
    await loadBarns()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openStall = (row) => {
  if (row) {
    Object.assign(stallForm, row)
  } else {
    Object.assign(stallForm, { id: null, code: '', name: '', barnId: null, stallType: '并列式', status: '可用' })
  }
  stallVisible.value = true
}

const submitStall = async () => {
  try {
    if (stallForm.id) {
      await stallApi.update(stallForm.id, {
        name: stallForm.name,
        barnId: stallForm.barnId,
        stallType: stallForm.stallType,
        status: stallForm.status
      })
    } else {
      await stallApi.create({ ...stallForm })
    }
    ElMessage.success('已保存')
    stallVisible.value = false
    await loadStalls()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const [b, c] = await Promise.all([barnApi.list({}), cowApi.list({})])
    barns.value = b
    cows.value = c
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadStalls()
})
</script>
