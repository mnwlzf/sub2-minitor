<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">SM</div>
        <div>
          <div class="brand-title">Sub Monitor</div>
          <div class="brand-subtitle">Vue3 + Element Plus</div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" class="nav-menu" router>
        <el-menu-item index="/overview">监控总览</el-menu-item>
        <el-menu-item index="/scheduler/tasks">定时任务</el-menu-item>
      </el-menu>

      <el-card shadow="never" class="sidebar-card">
        <template #header>运行状态</template>
        <div class="status-row">
          <span>Quartz</span>
          <el-tag type="success" size="small">运行中</el-tag>
        </div>
        <div class="status-row">
          <span>Redis</span>
          <el-tag type="success" size="small">已连接</el-tag>
        </div>
        <div class="status-row">
          <span>MySQL</span>
          <el-tag type="success" size="small">正常</el-tag>
        </div>
      </el-card>
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
      </header>

      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps<{
  title: string
  subtitle: string
}>()

const route = useRoute()

const activeMenu = computed(() => {
  if (route.path.startsWith('/scheduler/tasks')) {
    return '/scheduler/tasks'
  }
  return '/overview'
})
</script>
