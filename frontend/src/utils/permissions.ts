/**
 * 系统功能页签与权限定义清单
 *
 * 规范定义系统中的 6 大核心模块与 17 个子页签的路由、名称、图标与说明。
 */

export interface PermissionNode {
  id: string // 路由路径或权限唯一标识 (如 '/dashboard', '/ticket-list')
  name: string // 菜单/页签中文名称
  icon?: string
  description?: string
  children?: PermissionNode[]
}

export const SYSTEM_PERMISSION_TREE: PermissionNode[] = [
  {
    id: '/dashboard',
    name: '仪表盘 (Dashboard)',
    icon: 'Odometer',
    description: '系统整体运行概览、执行统计、待办审批与拦截监控大盘'
  },
  {
    id: '/tickets',
    name: '工单中心 (Ticket Center)',
    icon: 'Document',
    description: '工单申请、审批流转、AI 审查与工单生命周期追踪',
    children: [
      { id: '/ticket-list', name: '工单列表', description: '查看、检索与筛选系统历史与进行中工单' },
      { id: '/ticket-create', name: '新建 SQL 工单', description: '提交 DML/DDL 变更工单与执行事务级预校验 (Dry-Run)' },
      { id: '/ai-sql-review', name: 'AI 智能审核', description: '基于大模型的 SQL 性能诊断与安全规则扫描' }
    ]
  },
  {
    id: '/data',
    name: '数据操作与安全 (Data & Security)',
    icon: 'Search',
    description: '线上数据查询、脱敏规则配置与数据导出',
    children: [
      { id: '/data-query', name: '数据查询控制台', description: '在线 SQL 安全查询与大结果集分页浏览' },
      { id: '/data-masking', name: '动态脱敏配置', description: '手机号、身份证、银行卡等脱敏规则策略配置' }
    ]
  },
  {
    id: '/instances',
    name: '实例管理 (Instance Management)',
    icon: 'Coin',
    description: '数据库实例纳管、参数配置、会话监控与核心管控',
    children: [
      { id: '/instance-list', name: '实例列表', description: '数据库实例注册、连通性探测与环境分配' },
      { id: '/instance-config', name: '参数配置', description: '实例执行安全参数、工单定制扩展字段与必填约束配置' },
      { id: '/instance-sessions', name: '会话管理', description: '活跃连接监控、慢查询排查与会话强杀' },
      { id: '/instance-databases', name: '数据库管理', description: 'Schema 数据库清单与字符集管理' },
      { id: '/instance-accounts', name: '账号管理', description: '数据库原生用户账号与特权授权' },
      { id: '/instance-params', name: '全局参数查看', description: '数据库全局运行变量 (SHOW VARIABLES) 与平台安全策略' }
    ]
  },
  {
    id: '/perms',
    name: '权限与组织 (Permissions & Org)',
    icon: 'UserFilled',
    description: '业务资源组划分、人员组织与系统角色授权',
    children: [
      { id: '/resource-group-list', name: '业务资源组', description: '业务线与实例绑定划分、多资源组归属管理' },
      { id: '/user-list', name: '用户管理', description: '系统人员账号、姓名标识、组织分配与状态维护' },
      { id: '/role-list', name: '角色权限', description: '系统角色定义与页签功能权限矩阵配置' }
    ]
  },
  {
    id: '/system',
    name: '系统与流程 (System & Workflow)',
    icon: 'Operation',
    description: '工作流引擎 BPMN 可视化设计、授权证书与系统主题',
    children: [
      { id: '/workflow-designer', name: '流程设计与模板', description: 'BPMN 2.0 拓扑编排、排他网关条件路由与模板绑定' },
      { id: '/license', name: '授权证书', description: '商业版证书鉴权、授权节点数与到期状态' },
      { id: '/settings', name: '自定义主题', description: '系统外观风格、主色调与界面个性化定制' }
    ]
  }
]

/**
 * 获取所有具体子页签的路由路径清单（扁平数组）
 */
export const getAllTabKeys = (): string[] => {
  const keys: string[] = []
  const traverse = (nodes: PermissionNode[]) => {
    nodes.forEach(node => {
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      } else {
        keys.push(node.id)
      }
    })
  }
  traverse(SYSTEM_PERMISSION_TREE)
  return keys
}

/**
 * 预置角色标准页签权限包定义
 */
export const PRESET_PERMISSION_PACKAGES: Record<string, { name: string; icon: string; desc: string; paths: string[] }> = {
  ADMIN: {
    name: '超级管理员全权包',
    icon: '👑',
    desc: '拥有平台全部 17 个功能页签与特权管理权限',
    paths: ['*']
  },
  DBA: {
    name: '核心 DBA 运维管理包',
    icon: '💾',
    desc: '涵盖工单中心、数据查询、全量实例管理、业务资源组与 BPMN 流程设计',
    paths: [
      '/dashboard',
      '/ticket-list',
      '/ticket-create',
      '/ai-sql-review',
      '/data-query',
      '/data-masking',
      '/instance-list',
      '/instance-sessions',
      '/instance-databases',
      '/instance-accounts',
      '/instance-params',
      '/resource-group-list',
      '/workflow-designer'
    ]
  },
  DEV_LEAD: {
    name: '研发主管/开发组长审批包',
    icon: '🛡️',
    desc: '涵盖工单初审、SQL 创建、数据查询、资源组与审批流查看',
    paths: [
      '/dashboard',
      '/ticket-list',
      '/ticket-create',
      '/ai-sql-review',
      '/data-query',
      '/data-masking',
      '/resource-group-list',
      '/workflow-designer'
    ]
  },
  DEV: {
    name: '开发工程师自主包',
    icon: '💻',
    desc: '拥有个人工单提交、预执行校验、AI 诊断与数据查询控制台',
    paths: [
      '/dashboard',
      '/ticket-list',
      '/ticket-create',
      '/ai-sql-review',
      '/data-query'
    ]
  },
  AUDITOR: {
    name: '安全审计与合规包',
    icon: '🔍',
    desc: '专精于安全大盘监控、工单审核流追踪、动态脱敏配置与证书',
    paths: [
      '/dashboard',
      '/ticket-list',
      '/ai-sql-review',
      '/data-masking',
      '/license',
      '/settings'
    ]
  }
}

/**
 * 根据权限路径列表解析为直观的模块统计摘要
 */
export const getPermissionSummary = (permissions: string[] | string): { isAll: boolean; totalCount: number; modules: { name: string; count: number; total: number }[] } => {
  let list: string[] = []
  if (typeof permissions === 'string') {
    try {
      list = JSON.parse(permissions)
    } catch {
      list = [permissions]
    }
  } else if (Array.isArray(permissions)) {
    list = permissions
  }

  const allTabs = getAllTabKeys()
  if (list.includes('*')) {
    return {
      isAll: true,
      totalCount: allTabs.length,
      modules: SYSTEM_PERMISSION_TREE.map(m => ({
        name: m.name.split(' (')[0],
        count: m.children ? m.children.length : 1,
        total: m.children ? m.children.length : 1
      }))
    }
  }

  const modules = SYSTEM_PERMISSION_TREE.map(m => {
    const childKeys = m.children ? m.children.map(c => c.id) : [m.id]
    const matchedCount = childKeys.filter(k => list.includes(k)).length
    return {
      name: m.name.split(' (')[0],
      count: matchedCount,
      total: childKeys.length
    }
  }).filter(m => m.count > 0)

  return {
    isAll: list.length >= allTabs.length,
    totalCount: list.filter(k => allTabs.includes(k)).length,
    modules
  }
}
