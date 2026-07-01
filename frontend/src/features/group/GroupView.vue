<template>
  <AppShell title="分组查看" subtitle="按平台展示最新采集到的分组与倍率。">
    <el-card shadow="never" class="group-panel">
      <template #header>
        <div class="group-panel-head">
          <span>平台分组</span>
          <div class="group-toolbar">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索平台名称 / 地址"
              class="group-search"
              @keyup.enter="loadGroups"
            />
            <el-select v-model="filters.enabled" clearable placeholder="状态" class="group-status-filter">
              <el-option label="监控中" :value="true" />
              <el-option label="未监控" :value="false" />
            </el-select>
            <el-button type="primary" @click="loadGroups">搜索</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="group-platform-list">
        <div v-for="platform in platforms" :key="platform.platformId" class="group-platform-card">
          <div class="group-platform-head">
            <div class="group-platform-title">
              <div class="platform-avatar">{{ platformAvatar(platform.platformName) }}</div>
              <div>
                <div class="platform-name-line">
                  <span class="platform-name">{{ platform.platformName }}</span>
                  <el-tag size="small">{{ formatPlatformType(platform.platformType) }}</el-tag>
                  <el-tag :type="platform.enabled ? 'success' : 'info'" size="small">
                    {{ platform.enabled ? '监控中' : '未监控' }}
                  </el-tag>
                </div>
                <div class="platform-url">{{ platform.baseUrl }}</div>
              </div>
            </div>

            <div class="group-platform-stats">
              <div>
                <span>充值 / 到账</span>
                <strong>{{ formatRate(platform.rechargeRatio) }} / {{ formatRate(platform.discountRatio) }}</strong>
              </div>
              <div>
                <span>折算比例</span>
                <strong>{{ formatRate(platform.discountRatio) }}</strong>
              </div>
              <div>
                <span>分组数</span>
                <strong>{{ platform.groupCount ?? 0 }}</strong>
              </div>
              <div>
                <span>最后采集</span>
                <strong>{{ platform.lastCollectedAt ? formatDate(platform.lastCollectedAt) : '未采集' }}</strong>
              </div>
            </div>
          </div>

          <div v-if="platform.groups?.length" class="group-grid">
            <div
              v-for="group in platform.groups"
              :key="group.id"
              class="group-card"
              :class="{ changed: isChanged(group) }"
            >
              <div class="group-card-head">
                <strong>{{ group.groupName }}</strong>
                <span>平台 {{ formatRate(group.platformRate) }}</span>
              </div>
              <div class="group-card-foot">
                <span>{{ group.lastCollectedAt ? formatDate(group.lastCollectedAt) : '未采集' }}</span>
                <em>实际 {{ formatRate(group.actualRate) }}</em>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无分组" :image-size="120" />
        </div>
      </div>

      <div class="group-pagination">
        <span>Total {{ platforms.length }}</span>
        <el-select model-value="20/page" disabled class="group-page-size">
          <el-option label="20/page" value="20/page" />
        </el-select>
        <el-pagination :total="platforms.length" :page-size="20" layout="prev, pager, next" />
      </div>
    </el-card>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listCollectGroups } from './group.api'
import AppShell from '../../components/AppShell.vue'
import type { GroupItem, PlatformGroupItem } from './group.types'

const loading = ref(false)
const platforms = ref<PlatformGroupItem[]>([])
const filters = reactive<{ keyword: string; enabled: boolean | null }>({
  keyword: '',
  enabled: null,
})

const loadGroups = async () => {
  loading.value = true
  try {
    const data = await listCollectGroups({
      keyword: filters.keyword || undefined,
      enabled: filters.enabled,
    })
    platforms.value = data.items ?? []
  } catch (error) {
    ElMessage.error('加载分组失败')
  } finally {
    loading.value = false
  }
}

const platformAvatar = (name: string) => name?.slice(0, 1) || 'P'
const formatPlatformType = (value: string) => (value?.toUpperCase() === 'SUB2API' ? 'Sub2Api' : 'NewApi')
const formatRate = (value?: number | null) => Number(value ?? 0).toFixed(4)
const formatDate = (value: string) => value.replace('T', ' ').slice(0, 16).replaceAll('-', '/')
const isChanged = (group: GroupItem) => Number(group.platformRate ?? 0) !== Number(group.actualRate ?? 0)

onMounted(loadGroups)
</script>

