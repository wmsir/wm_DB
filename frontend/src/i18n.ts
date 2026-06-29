import { createI18n } from 'vue-i18n'

const messages = {
  zh: {
    menu: {
      dashboard: '大盘总览',
      ticketList: '工单中心',
      workflowDesigner: '流程设计器',
      license: '商业授权 (License)',
      aiSqlReview: 'AI 智能审查'
    },
    header: {
      welcome: '欢迎您',
      logout: '退出登录'
    }
  },
  en: {
    menu: {
      dashboard: 'Dashboard',
      ticketList: 'Ticket Center',
      workflowDesigner: 'Workflow Designer',
      license: 'Commercial License',
      aiSqlReview: 'AI SQL Review'
    },
    header: {
      welcome: 'Welcome',
      logout: 'Logout'
    }
  }
}

const i18n = createI18n({
  legacy: false, // 必须设置为 false 才能在 Composition API 中使用
  locale: localStorage.getItem('language') || 'zh', // 默认语言
  fallbackLocale: 'en',
  messages,
})

export default i18n
