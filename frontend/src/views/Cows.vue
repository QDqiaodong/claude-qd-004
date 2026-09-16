<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.barnId" placeholder="按牛舍" clearable style="width:170px">
          <el-option v-for="b in barns" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-select v-model="query.lactation" placeholder="按泌乳状态" clearable style="width:150px">
          <el-option v-for="l in lactations" :key="l" :label="l" :value="l" />
        </el-select>
        <el-select v-model="query.status" placeholder="按在栏状态" clearable style="width:140px">
          <el-option label="在栏" value="在栏" />
          <el-option label="离栏" value="离栏" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="耳号或昵称" clearable style="width:170px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCow()">新增奶牛</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 头</span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="earTag" label="耳号" width="120" />
      <el-table-column prop="nickname" label="昵称" width="110" />
      <el-table-column prop="breed" label="品种" width="110" />
      <el-table-column label="所在牛舍" width="150">
        <template #default="{ row }">{{ barnName(row.barnId) }}</template>
      </el-table-column>
      <el-table-column label="泌乳状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.lactation === '泌乳中' ? 'success' : row.lactation === '已淘汰' ? 'info' : 'warning'">
            {{ row.lactation }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="在栏状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '在栏' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCow(row)">调整</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '调整奶牛' : '新增奶牛'" width="480px">
      <el-form label-width="100px">
        <el-form-item label="耳号">
          <el-input v-model="form.earTag" :disabled="!!form.id" placeholder="如 DH-1009" />
        </el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="form.breed" /></el-form-item>
        <el-form-item label="泌乳状态">
          <el-select v-model="form.lactation" style="width:100%">
            <el-option v-for="l in lactations" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在牛舍">
          <el-select v-model="form.barnId" clearable placeholder="离栏可以不选" style="width:100%">
            <el-option
              v-for="b in usableBarns"
              :key="b.id"
              :label="`${b.name}（可容纳 ${b.capacity}）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="在栏状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="在栏" value="在栏" />
            <el-option label="离栏" value="离栏" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { barnApi, cowApi } from '../api'

const lactations = ['泌乳中', '干奶期', '待产', '已淘汰']

const barns = ref([])
const rows = ref([])
const loading = ref(false)
const query = reactive({ barnId: null, lactation: '', status: '', keyword: '' })

const visible = ref(false)
const form = reactive({
  id: null, earTag: '', nickname: '', breed: '荷斯坦',
  lactation: '泌乳中', barnId: null, status: '在栏'
})

const usableBarns = computed(() => barns.value.filter((b) => b.status === '在用'))
const barnName = (id) => (id ? barns.value.find((b) => b.id === id)?.name || '未归舍' : '未归舍')

const load = async () => {
  loading.value = true
  try {
    rows.value = await cowApi.list({
      barnId: query.barnId || undefined,
      lactation: query.lactation || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openCow = (row) => {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null, earTag: '', nickname: '', breed: '荷斯坦',
      lactation: '泌乳中', barnId: null, status: '在栏'
    })
  }
  visible.value = true
}

const submit = async () => {
  try {
    if (form.id) {
      await cowApi.update(form.id, {
        nickname: form.nickname,
        breed: form.breed,
        lactation: form.lactation,
        barnId: form.barnId,
        status: form.status
      })
    } else {
      await cowApi.create({ ...form })
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
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
  await load()
})
</script>
