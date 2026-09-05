// FWPT.CN 官方服务门户交互脚本
// 涵盖 稳骥 PacerSQL 与 斑马 ZebraERP 双旗舰产品交互体系

const platformCatalog = [
  // 稳骥产品线
  {
    name: '稳骥 PacerSQL 核心平台',
    brand: '稳骥 PacerSQL',
    category: '数据库自治云',
    desc: '千里良驹，稳压倒一切。高可用数据库协同治理与变更自治平台',
    icon: '🐎',
    colorBg: '#eff6ff',
    url: 'http://pacer.fwpt.cn/login',
    action: '直达平台'
  },
  {
    name: 'SQL 变更工单与灰度分批',
    brand: '稳骥 PacerSQL',
    category: '工单治理',
    desc: '大表变更平滑下发、0 锁表 0 抖动，微批流式渐进提交',
    icon: '📝',
    colorBg: '#ecfdf5',
    url: 'http://pacer.fwpt.cn/ticket-list',
    action: '进入工单'
  },
  {
    name: '在线数据查询与动态脱敏',
    brand: '稳骥 PacerSQL',
    category: '数据安全',
    desc: 'Web SQL 执行器、敏感字段自动掩码与数字盲水印防截屏泄密',
    icon: '🔍',
    colorBg: '#faf5ff',
    url: 'http://pacer.fwpt.cn/data-query',
    action: '安全查询'
  },
  {
    name: '多机分布式集群总览与主从路由',
    brand: '稳骥 PacerSQL',
    category: '高可用拓扑',
    desc: '101.35.100.169 (写) 与 39.97.158.22 (读) 多节点状态与 SpEL 路由',
    icon: '🖥️',
    colorBg: '#fffbeb',
    url: 'http://pacer.fwpt.cn/dashboard',
    action: '查看集群'
  },
  {
    name: '全量操作审计与合规大盘',
    brand: '稳骥 PacerSQL',
    category: '安全审计',
    desc: '全量 SQL 拦截日志、IP 白名单、国密 SM2/SM4、审计报表导出',
    icon: '🛡️',
    colorBg: '#fef2f2',
    url: 'http://pacer.fwpt.cn/audit-dashboard',
    action: '合规审计'
  },
  {
    name: '多渠道消息通知 (短信/企微/飞书)',
    brand: '稳骥 PacerSQL',
    category: '告警总线',
    desc: '阿里云/腾讯云短信、企业微信应用消息、钉钉/飞书催办通道',
    icon: '🔔',
    colorBg: '#f0f9ff',
    url: 'http://pacer.fwpt.cn/notification-config',
    action: '告警配置'
  },

  // 斑马产品线
  {
    name: '斑马 ZebraERP 核心平台',
    brand: '斑马 Zebra',
    category: '智能云仓 ERP',
    desc: '黑白条码视觉基因，全场景条码扫码、智能仓储一体化企业资源系统',
    icon: '🦓',
    colorBg: '#f4f4f5',
    url: 'http://zebra.fwpt.cn',
    action: '进入斑马'
  },
  {
    name: '商品条形码 (Barcode) 生成与打印',
    brand: '斑马 Zebra',
    category: '条码中心',
    desc: '标准 EAN-13、Code-128、GS1 编码，一键批量生成热敏不干胶标签',
    icon: '🏷️',
    colorBg: '#f4f4f5',
    url: 'http://zebra.fwpt.cn/barcode',
    action: '生成条码'
  },
  {
    name: 'PDA 激光扫码极速出入库核销',
    brand: '斑马 Zebra',
    category: 'WMS 仓储',
    desc: '0.2s 极速扫码响应，一物一码溯源，支持手机摄像头与硬件扫码枪',
    icon: '📦',
    colorBg: '#ecfdf5',
    url: 'http://zebra.fwpt.cn/wms/inbound',
    action: '扫码核销'
  },
  {
    name: '多仓智能实时库存动态盘点',
    brand: '斑马 Zebra',
    category: '库存管控',
    desc: '库位动态分配、智能缺货预警、跨仓自动波次调拨与进销存账务',
    icon: '📊',
    colorBg: '#fff7ed',
    url: 'http://zebra.fwpt.cn/inventory',
    action: '盘点看板'
  }
];

let selectedIndex = 0;
let filteredList = [...platformCatalog];

// 产品切换器 (Apple 级分段选择)
function selectProductTab(productKey) {
  const btnPacer = document.getElementById('tabBtnPacer');
  const btnZebra = document.getElementById('tabBtnZebra');
  const panelPacer = document.getElementById('panelPacer');
  const panelZebra = document.getElementById('panelZebra');

  if (productKey === 'pacer') {
    btnPacer.classList.add('active');
    btnZebra.classList.remove('active');
    panelPacer.style.display = 'block';
    panelPacer.classList.add('active');
    panelZebra.style.display = 'none';
    panelZebra.classList.remove('active');
  } else if (productKey === 'zebra') {
    btnZebra.classList.add('active');
    btnPacer.classList.remove('active');
    panelZebra.style.display = 'block';
    panelZebra.classList.add('active');
    panelPacer.style.display = 'none';
    panelPacer.classList.remove('active');
  }
}

// 快速下拉菜单
function toggleNavDropdown() {
  const menu = document.getElementById('navQuickMenu');
  if (menu) {
    menu.classList.toggle('show');
  }
}

// 点击外部关闭下拉
document.addEventListener('click', (e) => {
  const wrap = document.querySelector('.nav-dropdown-wrap');
  const menu = document.getElementById('navQuickMenu');
  if (wrap && menu && !wrap.contains(e.target)) {
    menu.classList.remove('show');
  }
});

// 跳转稳骥 PacerSQL
function jumpToPacer() {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://pacer.fwpt.cn${port}/login`;
  } else {
    // 本地或直接 IP 访问时的优雅降级
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

// 跳转斑马 ZebraERP
function jumpToZebra() {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://zebra.fwpt.cn${port}`;
  } else {
    // 若在本地或 IP 环境下提示
    alert('正在前往 斑马 ZebraERP (zebra.fwpt.cn) 平台！');
    window.location.href = `http://zebra.fwpt.cn`;
  }
}

function jumpToZebraModule(path) {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://zebra.fwpt.cn${port}${path}`;
  } else {
    alert(`正在前往 斑马 ZebraERP 模块 [${path}]！`);
    window.location.href = `http://zebra.fwpt.cn${path}`;
  }
}

// 页面滚动阴影
window.addEventListener('scroll', () => {
  const nav = document.getElementById('portalNav');
  if (nav) {
    if (window.scrollY > 20) {
      nav.classList.add('scrolled');
    } else {
      nav.classList.remove('scrolled');
    }
  }
});

// 全局快捷键 / 聚焦搜索
window.addEventListener('keydown', (e) => {
  const input = document.getElementById('searchInput');
  if (e.key === '/' && document.activeElement !== input) {
    e.preventDefault();
    input?.focus();
  }
});

// 搜索交互
document.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('searchInput');
  const box = document.getElementById('omniboxBox');
  const dropdown = document.getElementById('searchDropdown');
  const resultsList = document.getElementById('resultsList');
  const resultsCount = document.getElementById('resultsCount');
  const clearBtn = document.getElementById('clearSearchBtn');
  const triggerBtn = document.getElementById('btnSearchTrigger');

  if (triggerBtn && input) {
    triggerBtn.addEventListener('click', () => input.focus());
  }

  function renderResults() {
    if (!resultsList || !resultsCount) return;
    resultsList.innerHTML = '';
    resultsCount.textContent = `匹配到 ${filteredList.length} 个功能与模块`;

    if (filteredList.length === 0) {
      resultsList.innerHTML = '<div style="padding: 20px; text-align: center; color: #94a3b8; font-size: 13px;">未找到匹配项，按回车可直达 稳骥 PacerSQL 控制台</div>';
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
        window.location.href = item.url;
      });
      el.addEventListener('mouseover', () => {
        selectedIndex = idx;
        updateSelectedClass();
      });

      resultsList.appendChild(el);
    });
  }

  function updateSelectedClass() {
    if (!resultsList) return;
    const items = resultsList.querySelectorAll('.result-item');
    items.forEach((item, idx) => {
      if (idx === selectedIndex) {
        item.classList.add('is-selected');
      } else {
        item.classList.remove('is-selected');
      }
    });
  }

  if (input && box && dropdown) {
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
      if (clearBtn) clearBtn.style.display = q ? 'flex' : 'none';
      if (!q) {
        filteredList = [...platformCatalog];
      } else {
        filteredList = platformCatalog.filter(it => 
          it.name.toLowerCase().includes(q) || 
          it.desc.toLowerCase().includes(q) || 
          it.brand.toLowerCase().includes(q) || 
          it.category.toLowerCase().includes(q)
        );
      }
      selectedIndex = 0;
      renderResults();
    });

    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        input.value = '';
        clearBtn.style.display = 'none';
        filteredList = [...platformCatalog];
        selectedIndex = 0;
        renderResults();
        input.focus();
      });
    }

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
  }

  window.handleSearchSubmit = function() {
    if (filteredList.length > 0) {
      const item = filteredList[selectedIndex] || filteredList[0];
      window.location.href = item.url;
    } else {
      jumpToPacer();
    }
  };
});
