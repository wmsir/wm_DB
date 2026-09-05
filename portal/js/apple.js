
function jumpToPacer() {
  window.location.href = 'http://pacer.fwpt.cn/login';
}

function jumpToZebra() {
  window.location.href = 'http://zebra.fwpt.cn';
}

// 自动兼容本地 file:/// 打开与线上 Web 服务器路由
(function() {
  if (window.location.protocol === 'file:') {
    document.addEventListener('DOMContentLoaded', function() {
      document.querySelectorAll('a[data-file-href]').forEach(function(a) {
        a.href = a.getAttribute('data-file-href');
      });
    });
  }
})();
