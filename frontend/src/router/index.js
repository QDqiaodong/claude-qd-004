import { createRouter, createWebHistory } from 'vue-router'
import Barns from '../views/Barns.vue'
import Cows from '../views/Cows.vue'
import Shifts from '../views/Shifts.vue'
import Feeds from '../views/Feeds.vue'
import MilkTests from '../views/MilkTests.vue'

const routes = [
  { path: '/', redirect: '/barns' },
  { path: '/barns', component: Barns, meta: { title: '牛舍与挤奶位' } },
  { path: '/cows', component: Cows, meta: { title: '奶牛档案' } },
  { path: '/shifts', component: Shifts, meta: { title: '挤奶班次' } },
  { path: '/milk-tests', component: MilkTests, meta: { title: '原奶抽检' } },
  { path: '/feeds', component: Feeds, meta: { title: '饲料领用' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
