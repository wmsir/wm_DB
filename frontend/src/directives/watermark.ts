import type { Directive, DirectiveBinding } from 'vue';
import { jwtDecode } from 'jwt-decode';

interface DecodedToken {
  sub: string; // idCard
  realName: string;
  exp: number;
}

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

export const watermark: Directive = {
  mounted(el, binding) {
    // Create a wrapper to contain the watermark specifically, so we don't ruin the parent layout
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
