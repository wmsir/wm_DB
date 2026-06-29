<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">wmDB 完美数据库</div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <span>{{ t('menu.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/ticket-list">
          <span>{{ t('menu.ticketList') }}</span>
        </el-menu-item>
        <el-menu-item index="/workflow-designer">
          <span>{{ t('menu.workflowDesigner') }}</span>
        </el-menu-item>
        <el-menu-item index="/license">
          <span>{{ t('menu.license') }}</span>
        </el-menu-item>
        <el-menu-item index="/ai-sql-review">
          <span>{{ t('menu.aiSqlReview') }}</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <span>自定义主题</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-right">
          <el-dropdown @command="handleLanguageChange" style="margin-right: 20px; cursor: pointer;">
            <span class="el-dropdown-link">
              语言 / Language<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">中文</el-dropdown-item>
                <el-dropdown-item command="en">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span>{{ t('header.welcome') }}，{{ userRealName }}</span>
          <el-button type="text" @click="handleLogout" style="margin-left: 20px;">{{ t('header.logout') }}</el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useI18n } from 'vue-i18n'
import { ArrowDown } from '@element-plus/icons-vue'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const userRealName = computed(() => userStore.realName || '管理员')

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleLanguageChange = (lang: string) => {
  locale.value = lang
  localStorage.setItem('language', lang)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
}
.layout-aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  background-color: #2b3643;
}
.el-menu-vertical {
  border-right: none;
}
.layout-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}
.header-right {
  display: flex;
  align-items: center;
}
.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
