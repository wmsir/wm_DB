import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import monacoEditorPlugin from 'vite-plugin-monaco-editor'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    (monacoEditorPlugin as any).default({
      languageWorkers: ['editorWorkerService', 'css', 'html', 'json', 'typescript']
    })
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('monaco-editor')) return 'monaco-editor'
            if (id.includes('element-plus')) return 'element-plus'
            if (id.includes('bpmn-js')) return 'bpmn-js'
            if (id.includes('echarts')) return 'echarts'
            if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
            return 'vendor'
          }
        }
      }
    }
  }
})
