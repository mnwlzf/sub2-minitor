import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
import PlatformView from '../views/PlatformView.vue'
import SchedulerTaskView from '../views/SchedulerTaskView.vue'
import GroupView from '../views/GroupView.vue'
import BalanceView from '../views/BalanceView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/overview',
    },
    {
      path: '/overview',
      name: 'overview',
      component: OverviewView,
    },
    {
      path: '/platforms',
      name: 'platforms',
      component: PlatformView,
    },
    {
      path: '/scheduler/tasks',
      name: 'scheduler-tasks',
      component: SchedulerTaskView,
    },
    {
      path: '/groups',
      name: 'groups',
      component: GroupView,
    },
    {
      path: '/balances',
      name: 'balances',
      component: BalanceView,
    },
  ],
})

export default router
