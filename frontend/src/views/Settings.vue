<template>
  <div class="settings-container">
    <h2>主题设置</h2>
    <el-card shadow="hover">
      <el-form label-width="120px">
        <el-form-item label="自定义主题">
          <el-color-picker v-model="primaryColor" @change="handleColorChange" />
        </el-form-item>
        <el-form-item label="暗黑模式">
           <el-switch v-model="isDark" @change="toggleDark" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useDark, useToggle } from '@vueuse/core'

const isDark = useDark()
const toggleDark = useToggle(isDark)

const primaryColor = ref('#409EFF')

const handleColorChange = (color: string) => {
  if (color) {
    document.documentElement.style.setProperty('--el-color-primary', color)
    localStorage.setItem('primaryColor', color)
  }
}

onMounted(() => {
  const savedColor = localStorage.getItem('primaryColor')
  if (savedColor) {
    primaryColor.value = savedColor
    document.documentElement.style.setProperty('--el-color-primary', savedColor)
  }
})
</script>

<style scoped>
.settings-container {
  padding: 20px;
}
</style>
