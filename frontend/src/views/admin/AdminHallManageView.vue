<template>
  <div class="admin-layout">
    <AdminSidebar
      :items="sidebarItems"
      :activeSidebar="activeSidebar"
      :collapsed="sidebarCollapsed"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      @sidebar-click="handleSidebarClick"
    />
    <div class="admin-main">
      <el-card class="main-card" shadow="never">
        <div class="page-header">
          <span class="page-title">影厅管理</span>
          <el-button type="primary" @click="openAddDialog">新增影厅</el-button>
        </div>
        <el-table :data="halls" border stripe size="small" class="hall-table">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="capacity" label="容量" />
          <el-table-column prop="seatLayout" label="座位布局">
            <template #default="{ row }">
              <span>{{ seatLayoutSummary(row.seatLayout) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="screenType" label="银幕类型" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteHallRow(row.id)">删除</el-button>
              <el-button size="small" type="info" @click="openSeatDialog(row)">座位管理</el-button>
            </template>
          </el-table-column>
        </el-table>
        <!-- 新增/编辑影厅弹窗 -->
        <el-dialog v-model="showDialog" :title="dialogMode === 'add' ? '新增影厅' : '编辑影厅'" width="600px">
          <el-form :model="form" label-width="80px" ref="formRef">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type">
                <el-option label="标准厅" value="STANDARD" />
                <el-option label="IMAX" value="IMAX" />
                <el-option label="杜比" value="DOLBY" />
                <el-option label="VIP" value="VIP" />
              </el-select>
            </el-form-item>
            <el-form-item label="容量" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" />
            </el-form-item>
            <el-form-item label="座位布局" prop="seatLayout">
              <el-input v-model="form.seatLayout" type="textarea" :rows="4" placeholder="如: [[1,1,1],[1,0,1]] 或自定义JSON" />
            </el-form-item>
            <el-form-item label="银幕类型" prop="screenType">
              <el-input v-model="form.screenType" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showDialog = false">取消</el-button>
            <el-button type="primary" @click="submitForm">保存</el-button>
          </template>
        </el-dialog>
        <!-- 座位管理弹窗 -->
        <el-dialog v-model="showSeatDialog" title="座位管理" width="800px">
          <div>弹窗测试内容</div>
          <SeatLayoutEditor v-model="seatEditLayout" @save="saveSeatLayout" @close="showSeatDialog = false" />
        </el-dialog>
      </el-card>
      <el-card class="footer-card" shadow="never">
        <div class="footer-text">© 2024 Cinema Admin 后台管理系统</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { useRouter } from 'vue-router';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';
import { getAllHalls, createHall, updateHall, deleteHall } from '@/api/hall';
import SeatLayoutEditor from '@/components/SeatLayoutEditor.vue';

const activeSidebar = ref('影厅管理');
const sidebarCollapsed = ref(false);
const router = useRouter();

const halls = ref([]);
const showDialog = ref(false);
const dialogMode = ref('add');
const form = reactive({
  id: null,
  name: '',
  type: 'STANDARD',
  capacity: 100,
  seatLayout: '',
  screenType: ''
});
const formRef = ref();

const showSeatDialog = ref(false);
const seatEdit = reactive({
  id: null,
  layout: ''
});
const seatEditLayout = ref('');

const batchRows = ref(8);
const batchCols = ref(10);
const batchType = ref('STANDARD');

const importTemplates = ref([]);
const importTemplateName = ref('');

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

async function fetchHalls() {
  halls.value = await getAllHalls();
}

function openAddDialog() {
  dialogMode.value = 'add';
  Object.assign(form, { id: null, name: '', type: 'STANDARD', capacity: 100, seatLayout: '', screenType: '' });
  showDialog.value = true;
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  if (form.id !== row.id) {
    Object.assign(form, row);
  }
  showDialog.value = true;
}

async function submitForm() {
  if (dialogMode.value === 'add') {
    await createHall(form);
    ElMessage.success('新增成功');
  } else {
    await updateHall(form.id, form);
    ElMessage.success('修改成功');
  }
  showDialog.value = false;
  fetchHalls();
}

async function deleteHallRow(id) {
  await deleteHall(id);
  ElMessage.success('删除成功');
  fetchHalls();
}

function seatLayoutSummary(layout) {
  if (!layout) return '';
  try {
    const arr = JSON.parse(layout);
    return `行:${arr.length} 列:${arr[0]?.length || 0}`;
  } catch {
    return layout.length > 10 ? layout.slice(0, 10) + '...' : layout;
  }
}

function openSeatDialog(row) {
  console.log('openSeatDialog', row);
  seatEdit.id = row.id;
  seatEditLayout.value = typeof row.seatLayout === 'string' ? row.seatLayout : '';
  showSeatDialog.value = true;
  console.log(showSeatDialog.value)
  loadTemplates();
}

function saveSeatLayout() {
  seatEdit.layout = seatEditLayout.value;
  if (dialogMode.value === 'edit') {
    form.seatLayout = seatEdit.layout;
    // 自动统计非空座位数
    try {
      const arr = JSON.parse(seatEdit.layout || '[]');
      let count = 0;
      arr.forEach(row => {
        row.forEach(cell => {
          if (cell && typeof cell === 'object' && cell.type) count++;
        });
      });
      form.capacity = count;
    } catch {}
  }
  ElMessage.success('座位布局已保存到编辑区');
  showSeatDialog.value = false;
}

function seatTypeLabel(type) {
  if (type === 'STANDARD') return '标';
  if (type === 'VIP') return 'V';
  if (type === 'COUPLE') return '情';
  return '';
}

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

// 右键菜单
const typeMenu = reactive({ visible: false, i: 0, j: 0, x: 0, y: 0 });
function openTypeMenu(i, j, e) {
  typeMenu.visible = true;
  typeMenu.i = i;
  typeMenu.j = j;
  typeMenu.x = e.clientX;
  typeMenu.y = e.clientY;
  setTimeout(() => { document.addEventListener('click', closeTypeMenu, { once: true }); }, 0);
}
function closeTypeMenu() { typeMenu.visible = false; }
const typeMenuStyle = computed(() => ({ position: 'fixed', left: typeMenu.x + 'px', top: typeMenu.y + 'px', zIndex: 9999 }));
function setSeatType(type) {
  const arr = seatRows.value.map(row => row.map(cell => cell ? { ...cell } : null));
  if (type === 'EMPTY') {
    arr[typeMenu.i][typeMenu.j] = null;
  } else {
    arr[typeMenu.i][typeMenu.j] = { type };
  }
  seatRows.value = arr;
  closeTypeMenu();
}
function isSelected(i, j) { return typeMenu.visible && typeMenu.i === i && typeMenu.j === j; }

function saveSeatTemplate() {
  let data = seatEdit.layout;
  if (!data) {
    ElMessage.warning('当前没有可保存的座位布局');
    return;
  }
  const blob = new Blob([data], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = (form.name || 'seat_template') + '.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  ElMessage.success('模板已导出为json文件');
}

async function loadTemplates() {
  const context = import.meta.glob('@/frontend/hall_json/*.json', { query: '?raw', import: 'default' });
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
    seatEdit.layout = json;
    form.seatLayout = json;
    // 自动统计容量
    try {
      const arr = JSON.parse(json || '[]');
      let count = 0;
      arr.forEach(row => {
        row.forEach(cell => {
          if (cell && typeof cell === 'object' && cell.type && cell.type !== 'NULL') count++;
        });
      });
      form.capacity = count;
    } catch {}
    ElMessage.success('模板已导入');
  }
}

onMounted(() => {
  fetchHalls();
});
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #F5F7FA;
}
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.main-card {
  margin: 32px 24px 0 24px;
  border-radius: 8px;
  background: #fff;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.footer-card {
  margin: 16px 24px 24px 24px;
  background: #f5f7fa;
  border-radius: 8px;
  text-align: center;
  color: #86909C;
  font-size: 14px;
  box-shadow: none;
}
.footer-text {
  padding: 12px 0;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}
.hall-table {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}
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