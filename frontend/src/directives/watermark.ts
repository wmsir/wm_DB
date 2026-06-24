/**
 * 动态实名水印指令
 *
 * 用于在页面元素上铺满防删除的斜向半透明水印。
 * 水印内容通过解析 localStorage 中的 JWT Token 获取（格式：真实姓名_身份证后四位）。
 * 并使用 MutationObserver 监控 DOM 变化，防止恶意用户通过修改样式隐藏水印。
 */
import type { Directive, DirectiveBinding } from 'vue';
import { jwtDecode } from 'jwt-decode';

/**
 * 解析后的 JWT Token 数据结构
 */
interface DecodedToken {
  sub: string; // 身份证号码
  realName: string; // 真实姓名
  exp: number; // 过期时间
}

/**
 * 核心水印绘制和挂载逻辑
 *
 * @param el 挂载水印的目标 DOM 元素
 * @param _binding Vue 指令绑定对象
 */
const addWatermark = (el: HTMLElement, _binding: DirectiveBinding) => {
  const token = localStorage.getItem('wmdb_token');

  // For demo, if no token, use mock data. In real app, might just return or clear.
  let watermarkText = '游客_0000';

  if (token) {
    try {
      const decoded = jwtDecode<DecodedToken>(token);
      const realName = decoded.realName || '未知';
      const idCard = decoded.sub || '000000000000000000';
      const lastFour = idCard.slice(-4);
      watermarkText = `${realName}_${lastFour}`;
    } catch (e) {
      console.error('Failed to decode JWT for watermark', e);
    }
  } else {
      watermarkText = '演示_Mock';
  }

  // Create canvas to draw text
  const canvas = document.createElement('canvas');
  canvas.width = 250;
  canvas.height = 150;

  const ctx = canvas.getContext('2d');
  if (ctx) {
    ctx.rotate((-20 * Math.PI) / 180);
    ctx.font = '16px "PingFang SC", "Microsoft YaHei", sans-serif';
    ctx.fillStyle = 'rgba(200, 200, 200, 0.2)';
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.fillText(watermarkText, canvas.width / 6, canvas.height / 2);
  }

  // Set as background
  const base64Url = canvas.toDataURL('image/png');
  el.style.backgroundImage = `url('${base64Url}')`;
  el.style.backgroundRepeat = 'repeat';
  el.style.pointerEvents = 'none'; // Prevent watermark from blocking clicks
  // Ensure it covers the whole element
  el.style.position = 'absolute';
  el.style.top = '0';
  el.style.left = '0';
  el.style.width = '100%';
  el.style.height = '100%';
  el.style.zIndex = '9999';

  // Anti-tamper using MutationObserver
  // 使用 MutationObserver 监听 DOM 属性修改，防篡改
  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
        // Re-apply styles if tampered
        el.style.backgroundImage = `url('${base64Url}')`;
        el.style.display = 'block';
        el.style.visibility = 'visible';
        el.style.opacity = '1';
        el.style.zIndex = '9999';
      }
    }
  });

  observer.observe(el, { attributes: true, attributeFilter: ['style', 'class'] });

  // Store observer so it can be disconnected later
  (el as any).__wmdbObserver__ = observer;
};

/**
 * 暴露给 Vue 的自定义指令对象
 */
export const watermark: Directive = {
  mounted(el, binding) {
    // 创建一个专用的包装器来容纳水印，避免破坏父元素的布局
    const container = el as HTMLElement;
    container.style.position = container.style.position || 'relative';

    const watermarkDiv = document.createElement('div');
    addWatermark(watermarkDiv, binding);
    container.appendChild(watermarkDiv);

    (container as any).__watermarkDiv__ = watermarkDiv;
  },
  unmounted(el) {
    const container = el as any;
    if (container.__watermarkDiv__) {
      const observer = container.__watermarkDiv__.__wmdbObserver__;
      if (observer) {
        observer.disconnect();
      }
      if (container.__watermarkDiv__.parentNode) {
          container.__watermarkDiv__.parentNode.removeChild(container.__watermarkDiv__);
      }
    }
  }
};
