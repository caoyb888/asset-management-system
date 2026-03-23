<template>
  <div class="bpmn-viewer">
    <!-- 尝试使用 bpmn-js 渲染 -->
    <div v-if="useBpmnJs" ref="canvasRef" class="bpmn-canvas" />

    <!-- 降级：可视化流程节点 -->
    <div v-else class="bpmn-fallback">
      <div class="flow-nodes">
        <div
          v-for="(node, idx) in parsedNodes"
          :key="idx"
          class="flow-node"
          :class="`flow-node--${node.type}`"
        >
          <div class="flow-node__icon">
            <el-icon v-if="node.type === 'startEvent'" :size="20" color="#67c23a"><CircleCheck /></el-icon>
            <el-icon v-else-if="node.type === 'endEvent'" :size="20" color="#f56c6c"><CircleClose /></el-icon>
            <el-icon v-else-if="node.type === 'userTask'" :size="20" color="#409eff"><User /></el-icon>
            <el-icon v-else-if="node.type === 'exclusiveGateway'" :size="20" color="#e6a23c"><Connection /></el-icon>
            <el-icon v-else :size="20" color="#909399"><Document /></el-icon>
          </div>
          <div class="flow-node__info">
            <div class="flow-node__name">{{ node.name }}</div>
            <div v-if="node.assignee" class="flow-node__meta">审批人: {{ node.assignee }}</div>
            <div v-if="node.candidateGroups" class="flow-node__meta">候选组: {{ node.candidateGroups }}</div>
            <div v-if="node.condition" class="flow-node__meta">条件: {{ node.condition }}</div>
          </div>
          <div v-if="idx < parsedNodes.length - 1" class="flow-arrow">
            <el-icon :size="16"><Right /></el-icon>
          </div>
        </div>
      </div>

      <!-- XML 源码折叠面板 -->
      <el-collapse class="xml-collapse">
        <el-collapse-item title="查看 BPMN XML 源码">
          <pre class="xml-source">{{ xml }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { CircleCheck, CircleClose, User, Connection, Document, Right } from '@element-plus/icons-vue'

const props = defineProps<{
  xml: string
}>()

const canvasRef = ref<HTMLDivElement>()
const useBpmnJs = ref(false)

interface FlowNode {
  type: string
  name: string
  id: string
  assignee?: string
  candidateGroups?: string
  condition?: string
}

const parsedNodes = computed<FlowNode[]>(() => {
  if (!props.xml) return []
  return parseSimpleBpmn(props.xml)
})

function parseSimpleBpmn(xml: string): FlowNode[] {
  const nodes: FlowNode[] = []
  const parser = new DOMParser()
  const doc = parser.parseFromString(xml, 'text/xml')

  // 查找 process 元素
  const process = doc.querySelector('process')
  if (!process) return nodes

  // 收集所有节点
  const nodeMap = new Map<string, FlowNode>()
  for (const child of process.children) {
    const tag = child.localName
    if (['startEvent', 'endEvent', 'userTask', 'exclusiveGateway', 'serviceTask', 'scriptTask'].includes(tag)) {
      const node: FlowNode = {
        type: tag,
        name: child.getAttribute('name') || child.getAttribute('id') || tag,
        id: child.getAttribute('id') || '',
        assignee: child.getAttributeNS('http://flowable.org/bpmn', 'assignee')
          || child.getAttribute('flowable:assignee') || undefined,
        candidateGroups: child.getAttributeNS('http://flowable.org/bpmn', 'candidateGroups')
          || child.getAttribute('flowable:candidateGroups') || undefined,
      }
      nodeMap.set(node.id, node)
    }
  }

  // 收集 sequenceFlow 构建顺序
  const flows: { source: string; target: string; condition?: string }[] = []
  for (const child of process.children) {
    if (child.localName === 'sequenceFlow') {
      const condEl = child.querySelector('conditionExpression')
      flows.push({
        source: child.getAttribute('sourceRef') || '',
        target: child.getAttribute('targetRef') || '',
        condition: condEl?.textContent?.trim() || undefined,
      })
    }
  }

  // 从 startEvent 按 flow 顺序排列
  const startId = [...nodeMap.values()].find(n => n.type === 'startEvent')?.id
  if (!startId) return [...nodeMap.values()]

  const visited = new Set<string>()
  const queue = [startId]
  while (queue.length > 0) {
    const current = queue.shift()!
    if (visited.has(current)) continue
    visited.add(current)

    const node = nodeMap.get(current)
    if (node) nodes.push(node)

    // 找所有出边
    for (const flow of flows) {
      if (flow.source === current && !visited.has(flow.target)) {
        // 将条件附到目标节点
        if (flow.condition) {
          const target = nodeMap.get(flow.target)
          if (target) target.condition = flow.condition
        }
        queue.push(flow.target)
      }
    }
  }

  return nodes
}

// 尝试动态加载 bpmn-js
onMounted(async () => {
  try {
    // @ts-ignore bpmn-js is optional
    const BpmnJS = (await import('bpmn-js/lib/NavigatedViewer')).default
    useBpmnJs.value = true
    await nextTick()
    if (canvasRef.value && props.xml) {
      const viewer = new BpmnJS({ container: canvasRef.value })
      await viewer.importXML(props.xml)
      viewer.get('canvas').zoom('fit-viewport')
    }
  } catch {
    useBpmnJs.value = false
  }
})

watch(() => props.xml, async (newXml) => {
  if (useBpmnJs.value && canvasRef.value && newXml) {
    try {
      // @ts-ignore bpmn-js is optional
      const BpmnJS = (await import('bpmn-js/lib/NavigatedViewer')).default
      const viewer = new BpmnJS({ container: canvasRef.value })
      await viewer.importXML(newXml)
      viewer.get('canvas').zoom('fit-viewport')
    } catch {
      // fallback
    }
  }
})
</script>

<style scoped>
.bpmn-viewer {
  min-height: 200px;
}

.bpmn-canvas {
  height: 500px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.bpmn-fallback {
  padding: 16px 0;
}

.flow-nodes {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 16px;
}

.flow-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 2px solid #dcdfe6;
  background: #fff;
  min-width: 120px;
}

.flow-node--startEvent { border-color: #b3e19d; background: #f0f9eb; }
.flow-node--endEvent { border-color: #fab6b6; background: #fef0f0; }
.flow-node--userTask { border-color: #a0cfff; background: #ecf5ff; }
.flow-node--exclusiveGateway {
  border-color: #f3d19e;
  background: #fdf6ec;
  border-style: dashed;
  transform: rotate(0deg);
}

.flow-node__icon { flex-shrink: 0; }

.flow-node__name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.flow-node__meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.flow-arrow {
  color: #909399;
  margin: 0 4px;
}

.xml-collapse {
  margin-top: 12px;
}

.xml-source {
  background: #2d2d2d;
  color: #ccc;
  padding: 16px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
}
</style>
