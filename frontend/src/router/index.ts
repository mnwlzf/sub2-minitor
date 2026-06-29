import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
import PlatformView from '../views/PlatformView.vue'
import SchedulerTaskView from '../views/SchedulerTaskView.vue'

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
  ],
})

export default router
