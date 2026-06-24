<script setup lang="ts">
/**
 * 根组件入口
 * 全局挂载了 v-watermark 防止截图泄露
 */
</script>

<template>
  <div v-watermark style="height: 100vh; width: 100vw; overflow: hidden;">
    <router-view></router-view>
  </div>
</template>

<style scoped>
</style>
