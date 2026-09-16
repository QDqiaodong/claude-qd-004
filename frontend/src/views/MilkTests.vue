<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
          <span>已收班班次</span>
          <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="按日期" clearable size="small" style="width:160px" />
          <el-button size="small" type="primary" @click="loadAll">查询</el-button>
          <span style="color:#909399;font-size:12px">
            抽检只能挂在已收班的班次上；收班留下的公斤数化验室不许改少；不合格只在班次上留处置，不挡下一班。
          </span>
          <span style="margin-left:auto;color:#909399">共 {{ closedShifts.length }} 班</span>
        </div>
      </template>
      <el-table :data="closedShifts" border stripe size="small" v-loading="loading" row-key="id">
        <el-table-column prop="shiftNo" label="班次号" width="100" />
        <el-table-column prop="milkingDate" label="日期" width="105" />
        <el-table-column prop="period" label="班次" width="70" />
        <el-table-column label="挤奶位" width="130">
          <template #default="{ row }">{{ stallName(row.stallId) }}</template>
        </el-table-column>
        <el-table-column label="牛舍" width="120">
          <template #default="{ row }">{{ barnName(row.barnId) }}</template>
        </el-table-column>
        <el-table-column prop="milker" label="挤奶员" width="90" />
        <el-table-column label="收班产奶(公斤)" width="120">
          <template #default="{ row }">{{ row.milkKg }}</template>
        </el-table-column>
        <el-table-column label="抽检台账" min-width="150">
          <template #default="{ row }">
            <el-tag :type="stateTag(row.id).type" size="small">{{ stateTag(row.id).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="ledgerState(row.id) === 'none' || ledgerState(row.id) === 'await-retest'"
              link type="primary" @click="openSampling(row)"
            >{{ ledgerState(row.id) === 'await-retest' ? '复检抽检' : '抽检' }}</el-button>
            <el-button
              v-if="ledgerState(row.id) === 'await-disposition'"
              link type="warning" @click="openDisposition(row)"
            >留处置</el-button>
            <span v-else-if="isClosed(row.id)" style="color:#c0c4cc">已结案</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>抽检台账</span>
          <el-select v-model="ledgerShiftId" placeholder="只看某个班次" clearable size="small" filterable style="width:240px">
            <el-option
              v-for="s in shiftOptions"
              :key="s.id"
              :label="`${s.shiftNo}（${s.milkingDate} ${s.period}）`"
              :value="s.id"
            />
          </el-select>
          <el-button size="small" type="primary" @click="loadTests">查询</el-button>
          <span style="margin-left:auto;color:#909399">共 {{ tests.length }} 条</span>
        </div>
      </template>
      <el-table :data="tests" border stripe size="small" v-loading="loadingTests">
        <el-table-column label="班次号" width="110">
          <template #default="{ row }">{{ shiftMap[row.shiftId]?.shiftNo || `#${row.shiftId}` }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.recordType === '抽检' ? 'primary' : 'warning'" size="small">{{ row.recordType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="体细胞(个/mL)" width="140">
          <template #default="{ row }">{{ row.somaticCells ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="结论" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.result" :type="row.result === '合格' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="处置" width="90">
          <template #default="{ row }">{{ row.disposition || '—' }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="经手人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="samplingVisible" :title="samplingForm.retest ? `复检抽检 · ${samplingForm.shiftNo}` : `原奶抽检 · ${samplingForm.shiftNo}`" width="440px">
      <el-form label-width="120px">
        <el-form-item label="体细胞数">
          <el-input-number v-model="samplingForm.somaticCells" :min="1" :step="10000" :precision="0" style="width:220px" />
          <span style="margin-left:8px;color:#909399">个/mL</span>
        </el-form-item>
        <el-form-item label="抽检结论">
          <el-radio-group v-model="samplingForm.result">
            <el-radio label="合格">合格（结案）</el-radio>
            <el-radio label="不合格">不合格（须留处置）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="化验员"><el-input v-model="samplingForm.operator" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="samplingForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="samplingVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSampling">落台账</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispositionVisible" :title="`不合格处置 · ${dispositionForm.shiftNo}`" width="440px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom:12px"
        title="按场长规矩：处置只记在这条班次上，不拦下一班排班。"
      />
      <el-form label-width="120px">
        <el-form-item label="处置办法">
          <el-radio-group v-model="dispositionForm.disposition">
            <el-radio label="扣留">扣留</el-radio>
            <el-radio label="复检">复检（可再抽一次）</el-radio>
            <el-radio label="倒掉">倒掉</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="经手人"><el-input v-model="dispositionForm.operator" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="dispositionForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispositionVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDisposition">留处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { barnApi, milkTestApi, shiftApi, stallApi } from '../api'

const route = useRoute()
const loading = ref(false)
const loadingTests = ref(false)
const query = reactive({ date: '' })

const shifts = ref([])
const tests = ref([])
const barns = ref([])
const stalls = ref([])
const ledgerShiftId = ref(null)

const samplingVisible = ref(false)
const samplingForm = reactive({
  shiftId: null, shiftNo: '', somaticCells: 200000, result: '合格',
  operator: '', remark: '', retest: false
})
const dispositionVisible = ref(false)
const dispositionForm = reactive({
  shiftId: null, shiftNo: '', disposition: '扣留', operator: '', remark: ''
})

const shiftMap = computed(() => Object.fromEntries(shifts.value.map((s) => [s.id, s])))
const shiftOptions = computed(() => shifts.value.filter((s) => s.status === '已完成'))
const closedShifts = computed(() => shifts.value.filter((s) => s.status === '已完成'))

const barnName = (id) => (id ? barns.value.find((b) => b.id === id)?.name || `#${id}` : '—')
const stallName = (id) => (id ? stalls.value.find((s) => s.id === id)?.name || `#${id}` : '—')

/** 每个班次只认台账最后一条：none 未抽检 / await-disposition 不合格待处置 / await-retest 待复检抽检 / closed 结案 */
const ledgerByShift = computed(() => {
  const map = {}
  for (const t of [...tests.value].sort((a, b) => a.id - b.id)) {
    (map[t.shiftId] ||= []).push(t)
  }
  return map
})

const ledgerState = (shiftId) => {
  const ledger = ledgerByShift.value[shiftId]
  if (!ledger || ledger.length === 0) return 'none'
  const last = ledger[ledger.length - 1]
  if (last.recordType === '抽检') {
    return last.result === '合格' ? 'closed-pass' : 'await-disposition'
  }
  return last.disposition === '复检' ? 'await-retest' : 'closed-disposed'
}

const isClosed = (shiftId) => {
  const s = ledgerState(shiftId)
  return s === 'closed-pass' || s === 'closed-disposed'
}

const stateTag = (shiftId) => {
  switch (ledgerState(shiftId)) {
    case 'none':
      return { type: 'info', text: '未抽检' }
    case 'await-disposition':
      return { type: 'danger', text: '不合格·待处置' }
    case 'await-retest':
      return { type: 'warning', text: '已留复检·待抽检' }
    case 'closed-pass':
      return { type: 'success', text: '已结案·合格' }
    case 'closed-disposed': {
      const ledger = ledgerByShift.value[shiftId]
      return { type: 'success', text: `已结案·${ledger[ledger.length - 1].disposition}` }
    }
    default:
      return { type: 'info', text: '—' }
  }
}

const loadShifts = async () => {
  loading.value = true
  try {
    shifts.value = await shiftApi.list({ date: query.date || undefined })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const loadTests = async () => {
  loadingTests.value = true
  try {
    tests.value = await milkTestApi.list(ledgerShiftId.value || undefined)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loadingTests.value = false
  }
}

const loadAll = async () => {
  await Promise.all([loadShifts(), loadTests()])
}

const openSampling = (row) => {
  Object.assign(samplingForm, {
    shiftId: row.id,
    shiftNo: row.shiftNo,
    somaticCells: 200000,
    result: '合格',
    operator: '',
    remark: '',
    retest: ledgerState(row.id) === 'await-retest'
  })
  samplingVisible.value = true
}

const submitSampling = async () => {
  try {
    await milkTestApi.sampling({
      shiftId: samplingForm.shiftId,
      somaticCells: samplingForm.somaticCells,
      result: samplingForm.result,
      operator: samplingForm.operator,
      remark: samplingForm.remark
    })
    ElMessage.success(samplingForm.result === '合格' ? '抽检合格，已结案' : '已记不合格，请留处置')
    samplingVisible.value = false
    await loadTests()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openDisposition = (row) => {
  Object.assign(dispositionForm, {
    shiftId: row.id, shiftNo: row.shiftNo, disposition: '扣留', operator: '', remark: ''
  })
  dispositionVisible.value = true
}

const submitDisposition = async () => {
  try {
    await milkTestApi.disposition({
      shiftId: dispositionForm.shiftId,
      disposition: dispositionForm.disposition,
      operator: dispositionForm.operator,
      remark: dispositionForm.remark
    })
    ElMessage.success('处置已落台账')
    dispositionVisible.value = false
    await loadTests()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const [b, s] = await Promise.all([barnApi.list({}), stallApi.list({})])
    barns.value = b
    stalls.value = s
  } catch (e) {
    ElMessage.error(e.message)
  }
  query.date = route.query.date || new Date().toISOString().slice(0, 10)
  ledgerShiftId.value = route.query.shiftId ? Number(route.query.shiftId) : null
  await loadAll()
})
</script>
