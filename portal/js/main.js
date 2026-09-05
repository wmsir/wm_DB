// FWPT.CN 官网门户互动核心脚本
const catalog = [
  {
    name: '稳骥 PacerSQL 核心平台',
    category: '旗舰产品',
    desc: '千里良驹，稳压倒一切。高可用数据库协同治理与变更自治平台',
    icon: '🐎',
    colorBg: 'linear-gradient(135deg, #2563eb, #3b82f6)',
    path: '/login',
    action: '直达平台'
  },
  {
    name: 'SQL 变更工单中心',
    category: '变更管控',
    desc: 'SQL 提交、AI 语法预检、Dry-Run 影响推导与多级审批流转',
    icon: '📝',
    colorBg: 'linear-gradient(135deg, #059669, #10b981)',
    path: '/ticket-list',
    action: '进入工单'
  },
  {
    name: '在线数据查询与动态脱敏',
    category: '数据资产',
    desc: 'Web SQL 查询执行器、敏感字段自动掩码与数字水印防护',
    icon: '🔍',
    colorBg: 'linear-gradient(135deg, #7c3aed, #8b5cf6)',
    path: '/data-query',
    action: '安全查询'
  },
  {
    name: '多机分布式集群总览',
    category: '高可用拓扑',
    desc: '101.35.100.169 (主) 与 39.97.158.22 (从) 多节点状态与读写分离',
    icon: '🖥️',
    colorBg: 'linear-gradient(135deg, #d97706, #f59e0b)',
    path: '/dashboard',
    action: '查看集群'
  },
  {
    name: '安全合规与操作审计大盘',
    category: '安全审计',
    desc: '全量 SQL 拦截日志、IP 白名单、高危指令拦截报表导出',
    icon: '🛡️',
    colorBg: 'linear-gradient(135deg, #dc2626, #ef4444)',
    path: '/audit-dashboard',
    action: '合规审计'
  },
  {
    name: '消息通知与告警通道中心',
    category: '消息总线',
    desc: '短信 (阿里/腾讯)、企业微信、钉钉、飞书与电话告警配置',
    icon: '🔔',
    colorBg: 'linear-gradient(135deg, #0284c7, #0ea5e9)',
    path: '/notification-config',
    action: '告警配置'
  },
  {
    name: 'AI SQL 智能分析与优化',
    category: '智能预检',
    desc: '基于大模型的 SQL 索引推荐、执行计划慢查诊断与重构建议',
    icon: '🧠',
    colorBg: 'linear-gradient(135deg, #9333ea, #a855f7)',
    path: '/ai-review',
    action: 'AI 预检'
  }
];

let selectedIndex = 0;
let filteredList = [...catalog];

// 跳转到 PacerSQL (自适应二级域名或本地环境)
function jumpToPacer() {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://pacer.fwpt.cn${port}/login`;
  } else {
    window.location.href = `${window.location.protocol}//${host}${port}/login`;
  }
}

function jumpToPacerModule(path) {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://pacer.fwpt.cn${port}${path}`;
  } else {
    window.location.href = `${window.location.protocol}//${host}${port}${path}`;
  }
}

function scrollToId(id) {
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: 'smooth' });
}

// 导航栏滚动阴影
window.addEventListener('scroll', () => {
  const nav = document.getElementById('portalNav');
  if (window.scrollY > 20) {
    nav.classList.add('scrolled');
  } else {
    nav.classList.remove('scrolled');
  }
});

// 全局 / 快捷键聚焦搜索框
window.addEventListener('keydown', (e) => {
  const input = document.getElementById('searchInput');
  if (e.key === '/' && document.activeElement !== input) {
    e.preventDefault();
    input.focus();
  }
});

// 搜索交互
document.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('searchInput');
  const box = document.getElementById('searchBoxContainer');
  const dropdown = document.getElementById('searchDropdown');
  const resultsList = document.getElementById('resultsList');
  const resultsCount = document.getElementById('resultsCount');
  const clearBtn = document.getElementById('clearSearchBtn');
  const triggerBtn = document.getElementById('btnSearchTrigger');

  if (triggerBtn) {
    triggerBtn.addEventListener('click', () => input.focus());
  }

  function renderResults() {
    resultsList.innerHTML = '';
    resultsCount.textContent = `匹配到 ${filteredList.length} 个平台功能 / 模块`;

    if (filteredList.length === 0) {
      resultsList.innerHTML = '<div style="padding: 20px; text-align: center; color: #64748b; font-size: 13px;">未找到匹配模块，按回车可直达 PacerSQL 控制台</div>';
      return;
    }

    filteredList.forEach((item, idx) => {
      const el = document.createElement('div');
      el.className = `result-item ${idx === selectedIndex ? 'is-selected' : ''}`;
      el.innerHTML = `
        <div class="item-icon-wrap" style="background: ${item.colorBg}">
          <span>${item.icon}</span>
        </div>
        <div class="item-info">
          <div class="item-title-row">
            <span class="item-title">${item.name}</span>
            <span class="item-tag">${item.category}</span>
          </div>
          <div class="item-desc">${item.desc}</div>
        </div>
        <div class="item-action">
          <span>${item.action}</span>
          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </div>
      `;

      el.addEventListener('mousedown', () => {
        jumpToPacerModule(item.path);
      });
      el.addEventListener('mouseover', () => {
        selectedIndex = idx;
        updateSelectedClass();
      });

      resultsList.appendChild(el);
    });
  }

  function updateSelectedClass() {
    const items = resultsList.querySelectorAll('.result-item');
    items.forEach((item, idx) => {
      if (idx === selectedIndex) {
        item.classList.add('is-selected');
      } else {
        item.classList.remove('is-selected');
      }
    });
  }

  input.addEventListener('focus', () => {
    box.classList.add('has-focus');
    dropdown.style.display = 'block';
    renderResults();
  });

  input.addEventListener('blur', () => {
    setTimeout(() => {
      box.classList.remove('has-focus');
      dropdown.style.display = 'none';
    }, 200);
  });

  input.addEventListener('input', () => {
    const q = input.value.trim().toLowerCase();
    clearBtn.style.display = q ? 'flex' : 'none';
    if (!q) {
      filteredList = [...catalog];
    } else {
      filteredList = catalog.filter(it => 
        it.name.toLowerCase().includes(q) || 
        it.desc.toLowerCase().includes(q) || 
        it.category.toLowerCase().includes(q)
      );
    }
    selectedIndex = 0;
    renderResults();
  });

  clearBtn.addEventListener('click', () => {
    input.value = '';
    clearBtn.style.display = 'none';
    filteredList = [...catalog];
    selectedIndex = 0;
    renderResults();
    input.focus();
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (filteredList.length > 0) {
        selectedIndex = (selectedIndex + 1) % filteredList.length;
        updateSelectedClass();
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (filteredList.length > 0) {
        selectedIndex = (selectedIndex - 1 + filteredList.length) % filteredList.length;
        updateSelectedClass();
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      handleSearchSubmit();
    } else if (e.key === 'Escape') {
      input.value = '';
      dropdown.style.display = 'none';
      input.blur();
    }
  });

  window.handleSearchSubmit = function() {
    if (filteredList.length > 0) {
      const item = filteredList[selectedIndex] || filteredList[0];
      jumpToPacerModule(item.path);
    } else {
      jumpToPacer();
    }
  };
});
