// FWPT.CN 官方交互脚本 (Apple 风格)
function jumpToPacer() {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://pacer.fwpt.cn${port}/login`;
  } else {
    window.location.href = `${window.location.protocol}//${host}${port}/login`;
  }
}

function jumpToZebra() {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  if (host.includes('fwpt.cn')) {
    window.location.href = `http://zebra.fwpt.cn${port}`;
  } else {
    alert('正在前往 斑马 ZebraERP (zebra.fwpt.cn) 平台！');
    window.location.href = `http://zebra.fwpt.cn`;
  }
}
