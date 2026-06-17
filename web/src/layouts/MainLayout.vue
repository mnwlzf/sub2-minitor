<template>
  <el-container class="layout">
    <el-aside class="layout__aside" width="240px">
      <div class="layout__brand">
        <div class="layout__logo">S2</div>
        <div>
          <div class="layout__name">Sub2 Monitor</div>
          <div class="layout__desc">监控调度控制台</div>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        router
        class="layout__menu"
        :default-openeds="['/scheduler']"
      >
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>监控概览</span>
        </el-menu-item>
        <el-menu-item index="/platforms">
          <el-icon><Grid /></el-icon>
          <span>平台管理</span>
        </el-menu-item>
        <el-menu-item index="/balance-trend">
          <el-icon><TrendCharts /></el-icon>
          <span>余额查看</span>
        </el-menu-item>
        <el-menu-item index="/accounts">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/groups">
          <el-icon><Collection /></el-icon>
          <span>分组查看</span>
        </el-menu-item>
        <el-sub-menu index="/scheduler">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>定时任务</span>
          </template>
          <el-menu-item index="/scheduler/edit">任务编辑</el-menu-item>
          <el-menu-item index="/scheduler/logs">日志查看</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout__header">
        <div>
          <div class="layout__header-title">{{ route.meta.title }}</div>
          <div class="layout__header-subtitle">NewAPI / Sub2API 统一监控与调度入口</div>
        </div>
        <el-tag type="success" effect="light">Vue3 + TypeScript</el-tag>
      </el-header>

      <el-main class="layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Calendar, Collection, DataBoard, Grid, TrendCharts, User } from '@element-plus/icons-vue'

const route = useRoute()
const activePath = computed(() => route.path)
</script>

<style scoped>
.layout {
  height: 100vh;
  background: #f5f7fb;
}

.layout__aside {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
  background: #fff;
  color: #111827;
  border-right: 1px solid #e5e7eb;
}

.layout > .el-container {
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.layout__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 20px;
  border-bottom: 1px solid #eef2f7;
}

.layout__logo {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 10px;
  background: #409eff;
  font-weight: 700;
  color: #fff;
}

.layout__name {
  font-size: 16px;
  font-weight: 700;
}

.layout__desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.layout__menu {
  border-right: none;
  background: #fff;
}

.layout__menu :deep(.el-menu-item),
.layout__menu :deep(.el-sub-menu__title) {
  color: #334155;
}

.layout__menu :deep(.el-menu-item:hover),
.layout__menu :deep(.el-sub-menu__title:hover) {
  background: #f8fbff;
}

.layout__menu :deep(.el-menu-item.is-active) {
  background: rgb(64 158 255 / 12%);
  color: #409eff;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 1px 0 rgb(15 23 42 / 3%);
}

.layout__header-title {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.layout__header-subtitle {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.layout__main {
  height: calc(100vh - 72px);
  overflow: auto;
  padding: 24px;
  background: #f5f7fb;
}
</style>
