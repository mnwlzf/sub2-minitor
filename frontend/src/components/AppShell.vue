<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">S2</div>
        <div>
          <div class="brand-title">Sub2 Monitor</div>
          <div class="brand-subtitle">监控调度控制台</div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" class="nav-menu" router>
        <el-menu-item index="/overview">
          <el-icon><Monitor /></el-icon>
          <span>监控概览</span>
        </el-menu-item>
        <el-menu-item index="/platforms">
          <el-icon><Grid /></el-icon>
          <span>平台管理</span>
        </el-menu-item>
        <el-menu-item index="/balances">
          <el-icon><TrendCharts /></el-icon>
          <span>余额查看</span>
        </el-menu-item>
        <el-menu-item index="/accounts" disabled>
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/groups">
          <el-icon><Collection /></el-icon>
          <span>分组查看</span>
        </el-menu-item>
        <el-menu-item index="/mail" disabled>
          <el-icon><Message /></el-icon>
          <span>邮件设置</span>
        </el-menu-item>
        <el-sub-menu index="/scheduler">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>定时任务</span>
          </template>
          <el-menu-item index="/scheduler/tasks">任务编辑</el-menu-item>
          <el-menu-item index="/scheduler/logs" disabled>日志查看</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <div v-if="$slots.actions" class="topbar-actions">
          <slot name="actions" />
        </div>
        <div v-else class="topbar-actions">
          <el-tag type="success" effect="light">v1</el-tag>
          <el-button :icon="Position" plain @click="openInNewWindow">新窗口打开</el-button>
        </div>
      </header>

      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Calendar,
  Collection,
  Grid,
  Message,
  Monitor,
  Position,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'

defineProps<{
  title: string
  subtitle: string
}>()

const route = useRoute()

const activeMenu = computed(() => {
  if (route.path.startsWith('/platforms')) {
    return '/platforms'
  }
  if (route.path.startsWith('/balances')) {
    return '/balances'
  }
  if (route.path.startsWith('/scheduler/tasks')) {
    return '/scheduler/tasks'
  }
  if (route.path.startsWith('/groups')) {
    return '/groups'
  }
  return '/overview'
})

const openInNewWindow = () => {
  window.open(window.location.href, '_blank', 'noopener,noreferrer')
}
</script>
