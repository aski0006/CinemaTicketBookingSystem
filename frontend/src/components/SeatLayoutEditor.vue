<template>
  <div>
    <div style="margin-bottom: 12px;">
      <el-input-number v-model="batchRows" :min="1" placeholder="行数" style="width:100px;" />
      <el-input-number v-model="batchCols" :min="1" placeholder="列数" style="width:100px;margin-left:8px;" />
      <el-select v-model="batchType" style="width:120px;margin-left:8px;">
        <el-option label="标准座" value="STANDARD" />
        <el-option label="VIP座" value="VIP" />
        <el-option label="NULL" value="NULL" />
      </el-select>
      <el-button type="primary" @click="generateBatchLayout" style="margin-left:8px;">批量生成</el-button>
    </div>
    <div class="seat-preview seat-edit-area">
      <div class="seat-area-center">
        <div v-for="(row, i) in seatRows" :key="i" class="seat-row">
          <span v-for="(cell, j) in row" :key="j"
            :class="['seat-cell', cell && cell.type ? 'seat-'+cell.type.toLowerCase() : 'seat-aisle', {selected: isSelected(i,j)}]"
            @click="toggleSeat(i, j)"
            @contextmenu.prevent="openTypeMenu(i, j, $event)"
          >
            <template v-if="cell && cell.type === 'STANDARD'">
              <img src="/单人座位.svg" class="seat-svg" alt="标准座" />
            </template>
            <template v-else-if="cell && cell.type === 'VIP'">
              <img src="/VIP单人座位（选座位）-copy.svg" class="seat-svg" alt="VIP座" />
            </template>
            <template v-else>
              <!-- 空白/过道 -->
            </template>
          </span>
        </div>
      </div>
    </div>
    <el-dropdown v-if="typeMenu.visible" :style="typeMenuStyle" @command="setSeatType">
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="STANDARD">标准座</el-dropdown-item>
        <el-dropdown-item command="VIP">VIP座</el-dropdown-item>
        <el-dropdown-item command="EMPTY">空位</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <div style="margin: 12px 0; display: flex; gap: 8px; align-items: center;">
      <el-button type="primary" @click="$emit('save')">保存布局</el-button>
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="success" @click="saveSeatTemplate">保存为座位模板</el-button>
      <el-select v-model="importTemplateName" placeholder="选择模板" style="width:180px">
        <el-option v-for="tpl in importTemplates" :key="tpl.name" :label="tpl.name" :value="tpl.name" />
      </el-select>
      <el-button type="info" @click="handleImportTemplate">导入模板</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
const props = defineProps({
  layout: { type: String, default: '' }
});
const emit = defineEmits(['update:layout', 'save', 'close']);
const batchRows = ref(8);
const batchCols = ref(10);
const batchType = ref('STANDARD');
const importTemplates = ref([]);
const importTemplateName = ref('');
const seatRows = computed({
  get() {
    try {
      const arr = JSON.parse(props.layout || '[]');
      return arr.map(row => row.map(cell => {
        if (typeof cell === 'object' && cell !== null) return cell;
        if (cell === 1) return { type: 'STANDARD' };
        if (cell === 2) return { type: 'VIP' };
        if (cell === 3) return { type: 'NULL' };
        return { type: 'NULL' };
      }));
    } catch {
      return [];
    }
  },
  set(val) {
    emit('update:layout', JSON.stringify(val));
  }
});
function generateBatchLayout() {
  const arr = [];
  for (let i = 0; i < batchRows.value; i++) {
    const row = [];
    for (let j = 0; j < batchCols.value; j++) {
      row.push({ type: batchType.value });
    }
    arr.push(row);
  }
  seatRows.value = arr;
}
function toggleSeat(i, j) {
  const arr = seatRows.value.map(row => row.map(cell => cell ? { ...cell } : null));
  const cell = arr[i][j];
  if (!cell || cell === 0) {
    arr[i][j] = { type: 'STANDARD' };
  } else if (cell.type === 'STANDARD') {
    arr[i][j] = { type: 'VIP' };
  } else if (cell.type === 'VIP') {
    arr[i][j] = null;
  }
  seatRows.value = arr;
}
const typeMenu = ref({ visible: false, i: 0, j: 0, x: 0, y: 0 });
function openTypeMenu(i, j, e) {
  typeMenu.value.visible = true;
  typeMenu.value.i = i;
  typeMenu.value.j = j;
  typeMenu.value.x = e.clientX;
  typeMenu.value.y = e.clientY;
  setTimeout(() => { document.addEventListener('click', closeTypeMenu, { once: true }); }, 0);
}
function closeTypeMenu() { typeMenu.value.visible = false; }
const typeMenuStyle = computed(() => ({ position: 'fixed', left: typeMenu.value.x + 'px', top: typeMenu.value.y + 'px', zIndex: 9999 }));
function setSeatType(type) {
  const arr = seatRows.value.map(row => row.map(cell => cell ? { ...cell } : null));
  if (type === 'EMPTY') {
    arr[typeMenu.value.i][typeMenu.value.j] = null;
  } else {
    arr[typeMenu.value.i][typeMenu.value.j] = { type };
  }
  seatRows.value = arr;
  closeTypeMenu();
}
function isSelected(i, j) { return typeMenu.value.visible && typeMenu.value.i === i && typeMenu.value.j === j; }
function saveSeatTemplate() {
  let data = props.layout;
  if (!data) {
    ElMessage.warning('当前没有可保存的座位布局');
    return;
  }
  const blob = new Blob([data], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'seat_template.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  ElMessage.success('模板已导出为json文件');
}
async function loadTemplates() {
  const context = import.meta.glob('/frontend/hall_json/*.json', { query: '?raw', import: 'default' });
  importTemplates.value = [];
  for (const path in context) {
    const name = path.split('/').pop();
    importTemplates.value.push({ name, path });
  }
}
async function handleImportTemplate() {
  if (!importTemplateName.value) {
    ElMessage.warning('请选择模板');
    return;
  }
  const context = import.meta.glob('/frontend/hall_json/*.json', { query: '?raw', import: 'default' });
  const path = importTemplates.value.find(t => t.name === importTemplateName.value)?.path;
  if (!path) return;
  const loader = context[path];
  if (loader) {
    const json = await loader();
    emit('update:layout', json);
    ElMessage.success('模板已导入');
  }
}
watch(() => props.layout, () => {}, { immediate: true });
onMounted(() => { loadTemplates(); });
</script>
<style scoped>
.seat-preview {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.seat-area-center {
  display: inline-block;
  margin: 0 auto;
}
.seat-row {
  display: flex;
  margin-bottom: 2px;
  justify-content: center;
}
.seat-cell {
  display: inline-flex;
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  margin-right: 4px;
  border-radius: 4px;
  background: #fff;
  color: #333;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border: 1px solid #e5e6eb;
}
.seat-cell:last-child { margin-right: 0; }
.seat-aisle {
  background: transparent !important;
  border: none !important;
  pointer-events: none;
}
.seat-svg {
  width: 24px;
  height: 24px;
  display: block;
  margin: 0 auto;
}
.selected { outline: 2px solid #faad14; z-index: 2; }
</style> 