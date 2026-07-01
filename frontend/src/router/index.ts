import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
import PlatformView from '../views/PlatformView.vue'
import SchedulerTaskView from '../views/SchedulerTaskView.vue'
import GroupView from '../views/GroupView.vue'
import BalanceView from '../views/BalanceView.vue'
import AccountView from '../features/account/AccountView.vue'
import MailView from '../features/mail/MailView.vue'

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
    {
      path: '/accounts',
      name: 'accounts',
      component: AccountView,
    },
    {
      path: '/mail',
      name: 'mail',
      component: MailView,
    },
  ],
})

export default router
