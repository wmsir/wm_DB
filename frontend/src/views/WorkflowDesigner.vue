<template>
  <div class="workflow-designer-container">
    <div class="toolbar">
      <el-button type="primary" @click="deployProcess">部署审批流</el-button>
    </div>
    <div ref="canvas" class="canvas"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
// @ts-ignore
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const canvas = ref<HTMLElement | null>(null)
let modeler: any = null

const initialDiagram = `
<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" id="sample-diagram" targetNamespace="http://bpmn.io/schema/bpmn" xsi:schemaLocation="http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd">
  <bpmn2:process id="Process_1" isExecutable="false">
    <bpmn2:startEvent id="StartEvent_1" />
  </bpmn2:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="192" y="82" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn2:definitions>
`

onMounted(async () => {
  if (canvas.value) {
    modeler = new BpmnModeler({
      container: canvas.value,
      keyboard: {
        bindTo: document
      }
    })

    try {
      await modeler.importXML(initialDiagram)
      const canvasEl = modeler.get('canvas')
      canvasEl.zoom('fit-viewport')
    } catch (err) {
      console.error('BPMN Modeler instantiation failed', err)
      ElMessage.error('无法加载工作流画板')
    }
  }
})

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy()
  }
})

const deployProcess = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    console.log('Deployed XML:', xml)
    // In reality, you would send this XML string to the backend to deploy using Flowable's RepositoryService
    ElMessage.success('工作流已成功生成并挂载至后端（模拟演示）')
  } catch (err) {
    ElMessage.error('获取 XML 失败')
  }
}
</script>

<style scoped>
.workflow-designer-container {
  height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  padding: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.canvas {
  flex: 1;
  background-color: #fff;
}

/* Ensure bpmn.js overlays are styled correctly in the context of our app */
:deep(.bjs-powered-by) {
  display: none;
}
</style>
