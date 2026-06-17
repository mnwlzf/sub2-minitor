import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import DashboardView from '@/views/DashboardView.vue'
import AccountManageView from '@/views/AccountManageView.vue'
import PlatformManageView from '@/views/PlatformManageView.vue'
import BalanceTrendView from '@/views/BalanceTrendView.vue'
import PlatformGroupView from '@/views/PlatformGroupView.vue'
import SchedulerEditView from '@/views/SchedulerEditView.vue'
import SchedulerLogView from '@/views/SchedulerLogView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '监控概览' }
        },
        {
          path: 'platforms',
          name: 'platforms',
          component: PlatformManageView,
          meta: { title: '平台管理' }
        },
        {
          path: 'balance-trend',
          name: 'balance-trend',
          component: BalanceTrendView,
          meta: { title: '余额查看' }
        },
        {
          path: 'accounts',
          name: 'accounts',
          component: AccountManageView,
          meta: { title: '账号管理' }
        },
        {
          path: 'groups',
          name: 'groups',
          component: PlatformGroupView,
          meta: { title: '分组查看' }
        },
        {
          path: 'scheduler/edit',
          name: 'scheduler-edit',
          component: SchedulerEditView,
          meta: { title: '定时任务 / 任务编辑', schedulerActive: true }
        },
        {
          path: 'scheduler/logs',
          name: 'scheduler-logs',
          component: SchedulerLogView,
          meta: { title: '定时任务 / 日志查看', schedulerActive: true }
        }
      ]
    }
  ]
})

export default router
