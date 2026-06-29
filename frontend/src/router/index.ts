import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
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
      path: '/scheduler/tasks',
      name: 'scheduler-tasks',
      component: SchedulerTaskView,
    },
  ],
})

export default router
