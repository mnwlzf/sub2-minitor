<template>
  <AppShell
    title="监控总览"
    subtitle="前后端分离监控台，当前优先打通 Quartz 定时任务管理。"
  >
    <template #actions>
      <el-button @click="loadHealth">刷新</el-button>
      <el-button type="primary" @click="goScheduler">任务管理</el-button>
    </template>

    <section class="hero-grid">
      <el-card shadow="never" class="hero-card strong-card">
        <div class="card-eyebrow">系统状态</div>
        <div class="hero-line">
          <span class="big-number">{{ healthLabel }}</span>
          <el-tag :type="healthOk ? 'success' : 'danger'" effect="dark">
            {{ healthOk ? '在线' : '异常' }}
          </el-tag>
        </div>
        <p>后端健康检查接口已接通，下一步继续补平台、账号和采集结果页面。</p>
      </el-card>

      <el-card shadow="never" class="metric-card">
        <div class="card-eyebrow">后端接口</div>
        <div class="big-number small">/api</div>
        <div class="card-foot">当前通过 Vite 代理访问 Spring Boot</div>
      </el-card>

      <el-card shadow="never" class="metric-card">
        <div class="card-eyebrow">调度模块</div>
        <div class="big-number small">Quartz</div>
        <div class="card-foot">JDBC JobStore 已启用</div>
      </el-card>

      <el-card shadow="never" class="metric-card">
        <div class="card-eyebrow">当前重点</div>
        <div class="big-number small">任务联调</div>
        <div class="card-foot">优先完成任务列表、编辑、启停与手动执行</div>
      </el-card>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import { http } from '../api/http'

const router = useRouter()
const healthOk = ref(false)

const healthLabel = computed(() => (healthOk.value ? 'Healthy' : 'Unknown'))

const goScheduler = () => {
  router.push('/scheduler/tasks')
}

const loadHealth = async () => {
  try {
    const { data } = await http.get('/health')
    healthOk.value = Boolean(data?.success)
  } catch (error) {
    healthOk.value = false
    ElMessage.error('后端健康检查失败')
  }
}

onMounted(loadHealth)
</script>
