<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="按日期" clearable style="width:160px" />
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:140px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="query.milker" placeholder="挤奶员" clearable style="width:150px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openShift">排一班</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 班</span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="shiftNo" label="班次号" width="110" />
      <el-table-column prop="milkingDate" label="日期" width="115" />
      <el-table-column prop="period" label="班次" width="80" />
      <el-table-column label="挤奶位" width="140">
        <template #default="{ row }">{{ stallName(row.stallId) }}</template>
      </el-table-column>
      <el-table-column label="牛舍" width="130">
        <template #default="{ row }">{{ barnName(row.barnId) }}</template>
      </el-table-column>
      <el-table-column prop="milker" label="挤奶员" width="100" />
      <el-table-column label="时段" width="130">
        <template #default="{ row }">{{ hm(row.startMin) }}-{{ hm(row.endMin) }}</template>
      </el-table-column>
      <el-table-column label="产奶(公斤)" width="110">
        <template #default="{ row }">
          <span v-if="row.milkKg != null">{{ row.milkKg }}</span>
          <span v-else style="color:#c0c4cc">未登记</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === '待开挤'" link type="primary" @click="advance(row, 'start')">开挤</el-button>
          <el-button v-if="row.status === '挤奶中'" link type="success" @click="openDone(row)">收班</el-button>
          <el-button
            v-if="row.status === '待开挤' || row.status === '挤奶中'"
            link
            type="danger"
            @click="cancel(row)"
          >取消</el-button>
          <el-button v-if="row.status === '已完成'" link type="primary" @click="goMilkTest(row)">去抽检</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="shiftVisible" title="排一班" width="520px">
      <el-form label-width="110px">
        <el-form-item label="挤奶日期">
          <el-date-picker v-model="shiftForm.milkingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="shiftForm.period">
            <el-radio v-for="p in ['早班', '中班', '晚班']" :key="p" :label="p">{{ p }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="挤奶位">
          <el-select v-model="shiftForm.stallId" placeholder="只列可用的挤奶位" style="width:100%">
            <el-option
              v-for="s in usableStalls"
              :key="s.id"
              :label="`${s.name}（${s.code} · ${s.stallType}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挤哪个牛舍">
          <el-select v-model="shiftForm.barnId" placeholder="只列在用的牛舍" style="width:100%">
            <el-option
              v-for="b in usableBarns"
              :key="b.id"
              :label="`${b.name}（泌乳牛 ${lactatingCount(b.id)} 头）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挤奶员"><el-input v-model="shiftForm.milker" /></el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="shiftForm.start" start="00:00" step="00:30" end="23:30" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="shiftForm.end" start="00:00" step="00:30" end="23:30" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShift">排班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="doneVisible" title="收班登记产量" width="420px">
      <el-form label-width="130px">
        <el-form-item label="本班产奶(公斤)">
          <el-input-number v-model="doneMilk" :min="0" :step="10" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="doneVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDone">收班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { barnApi, cowApi, shiftApi, stallApi } from '../api'

const router = useRouter()

const statuses = ['待开挤', '挤奶中', '已完成', '已取消']

const rows = ref([])
const barns = ref([])
const stalls = ref([])
const cows = ref([])
const loading = ref(false)
const query = reactive({ date: '', status: '', milker: '' })

const shiftVisible = ref(false)
const shiftForm = reactive({
  milkingDate: '', period: '早班', stallId: null, barnId: null,
  milker: '', start: '05:00', end: '07:00'
})
const doneVisible = ref(false)
const doneMilk = ref(0)
const doneId = ref(null)

const usableStalls = computed(() => stalls.value.filter((s) => s.status === '可用'))
const usableBarns = computed(() => barns.value.filter((b) => b.status === '在用'))

const hm = (min) => `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
const toMin = (t) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5))
const barnName = (id) => (id ? barns.value.find((b) => b.id === id)?.name || `#${id}` : '未选')
const stallName = (id) => (id ? stalls.value.find((s) => s.id === id)?.name || `#${id}` : '未选')
const lactatingCount = (barnId) =>
  cows.value.filter((c) => c.barnId === barnId && c.status === '在栏' && c.lactation === '泌乳中').length
const tagType = (s) =>
  s === '已完成' ? 'success' : s === '挤奶中' ? 'warning' : s === '已取消' ? 'info' : ''

const load = async () => {
  loading.value = true
  try {
    rows.value = await shiftApi.list({
      date: query.date || undefined,
      status: query.status || undefined,
      milker: query.milker || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openShift = () => {
  Object.assign(shiftForm, {
    milkingDate: new Date().toISOString().slice(0, 10),
    period: '早班', stallId: null, barnId: null, milker: '', start: '05:00', end: '07:00'
  })
  shiftVisible.value = true
}

const submitShift = async () => {
  if (!shiftForm.start || !shiftForm.end) {
    ElMessage.warning('请把时段选完整')
    return
  }
  try {
    await shiftApi.open({
      milkingDate: shiftForm.milkingDate,
      period: shiftForm.period,
      stallId: shiftForm.stallId,
      barnId: shiftForm.barnId,
      milker: shiftForm.milker,
      startMin: toMin(shiftForm.start),
      endMin: toMin(shiftForm.end)
    })
    ElMessage.success('这一班排上了')
    shiftVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const advance = async (row, action) => {
  try {
    await shiftApi.advance(row.id, action, null)
    ElMessage.success('已更新')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openDone = (row) => {
  doneId.value = row.id
  doneMilk.value = 0
  doneVisible.value = true
}

const submitDone = async () => {
  try {
    await shiftApi.advance(doneId.value, 'done', doneMilk.value)
    ElMessage.success('已收班')
    doneVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const cancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消班次 ${row.shiftNo}？`, '提示')
  } catch {
    return
  }
  advance(row, 'cancel')
}

const goMilkTest = (row) => {
  router.push({ path: '/milk-tests', query: { shiftId: row.id, date: row.milkingDate } })
}

onMounted(async () => {
  try {
    const [b, s, c] = await Promise.all([barnApi.list({}), stallApi.list({}), cowApi.list({})])
    barns.value = b
    stalls.value = s
    cows.value = c
  } catch (e) {
    ElMessage.error(e.message)
  }
  query.date = new Date().toISOString().slice(0, 10)
  await load()
})
</script>
