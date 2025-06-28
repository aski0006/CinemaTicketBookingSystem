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
          <span class="page-title">今日场次管理</span>
          <el-button type="primary" @click="autoArrangeSessions" :disabled="!todayMovies.length || !halls.length">自动排片</el-button>
        </div>
        <div v-if="loading" class="loading-bar">加载中...</div>
        <div v-else>
          <el-alert v-if="!todayMovies.length" type="warning" show-icon title="今日还未设计上映影片" class="empty-alert" />
          <template v-else>
            <div class="movie-list">
              <div v-for="movie in todayMovies" :key="movie.id" class="movie-item">
                <img :src="movie.posterUrl || movie.poster_url" class="movie-poster" />
                <div class="movie-info">
                  <div class="movie-title">{{ movie.title }}</div>
                  <div class="movie-meta">时长：{{ movie.duration }}分钟 | 评分：{{ movie.rating || '--' }}</div>
                  <el-button size="small" @click="openManualDialog(movie)">手动排片</el-button>
                  <el-button size="small" type="danger" @click="removeTodayMovie(movie.id)" style="margin-left: 8px;">移除今日播映</el-button>
                </div>
              </div>
            </div>
            <el-divider>今日场次安排</el-divider>
            <el-table :data="sessions" border stripe size="small" class="session-table">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="movieTitle" label="影片" />
              <el-table-column prop="hallName" label="影厅" />
              <el-table-column prop="startTime" label="开始时间" />
              <el-table-column prop="endTime" label="结束时间" />
              <el-table-column prop="price" label="票价" />
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteSessionRow(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </div>
        <!-- 手动排片对话框 -->
        <el-dialog v-model="showManualDialog" title="手动排片" width="500px">
          <el-form :model="manualForm" label-width="80px" ref="manualFormRef">
            <el-form-item label="影厅" prop="hallId">
              <el-select v-model="manualForm.hallId" placeholder="请选择影厅">
                <el-option v-for="hall in halls" :key="hall.id" :label="hall.name" :value="hall.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker v-model="manualForm.startTime" placeholder="选择时间" format="HH:mm" />
            </el-form-item>
            <el-form-item label="票价" prop="price">
              <el-input-number v-model="manualForm.price" :min="1" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showManualDialog = false">取消</el-button>
            <el-button type="primary" @click="submitManualSession">保存</el-button>
          </template>
        </el-dialog>
        <!-- 编辑场次对话框 -->
        <el-dialog v-model="showEditDialog" title="编辑场次" width="500px">
          <el-form :model="editForm" label-width="80px" ref="editFormRef">
            <el-form-item label="影厅" prop="hallId">
              <el-select v-model="editForm.hallId" placeholder="请选择影厅">
                <el-option v-for="hall in halls" :key="hall.id" :label="hall.name" :value="hall.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker v-model="editForm.startTime" placeholder="选择时间" format="HH:mm" />
            </el-form-item>
            <el-form-item label="票价" prop="price">
              <el-input-number v-model="editForm.price" :min="1" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showEditDialog = false">取消</el-button>
            <el-button type="primary" @click="submitEditSession">保存</el-button>
          </template>
        </el-dialog>
      </el-card>
      <el-card class="footer-card" shadow="never">
        <div class="footer-text">© 2024 Cinema Admin 后台管理系统</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { useRouter } from 'vue-router';
import { getTodayMovies } from '@/api/movie';
import { getAllHalls } from '@/api/hall';
import service from '@/api/request';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';

const activeSidebar = ref('场次管理');
const sidebarCollapsed = ref(false);
const router = useRouter();

const todayMovies = ref([]);
const halls = ref([]);
const sessions = ref([]); // 本地维护今日场次
const loading = ref(true);

// 手动排片相关
const showManualDialog = ref(false);
const manualForm = reactive({
  movieId: null,
  hallId: null,
  startTime: '',
  price: 50
});
const manualFormRef = ref();

// 编辑场次相关
const showEditDialog = ref(false);
const editForm = reactive({
  id: null,
  movieId: null,
  hallId: null,
  startTime: '',
  price: 50
});
const editFormRef = ref();

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

async function fetchData() {
  loading.value = true;
  // 获取今日上映影片
  const res = await getTodayMovies();
  todayMovies.value = res.movies || [];
  // 获取所有影厅
  halls.value = await getAllHalls();
  loading.value = false;
}

function openManualDialog(movie) {
  manualForm.movieId = movie.id;
  manualForm.hallId = null;
  manualForm.startTime = '';
  manualForm.price = 50;
  showManualDialog.value = true;
}

async function submitManualSession() {
  // 构造场次数据
  const movie = todayMovies.value.find(m => m.id === manualForm.movieId);
  const hall = halls.value.find(h => h.id === manualForm.hallId);
  if (!movie || !hall) return ElMessage.error('请选择影片和影厅');
  const start = manualForm.startTime;
  const duration = movie.duration || 120;
  // 计算结束时间
  const [h, m] = start.split(':').map(Number);
  const startDate = new Date();
  startDate.setHours(h, m, 0, 0);
  const endDate = new Date(startDate.getTime() + duration * 60000);
  const endTime = `${String(endDate.getHours()).padStart(2, '0')}:${String(endDate.getMinutes()).padStart(2, '0')}`;
  const sessionData = {
    movieId: movie.id,
    hallId: hall.id,
    startTime: start,
    endTime,
    price: manualForm.price
  };
  await service.post('/api/sessions', sessionData);
  ElMessage.success('场次创建成功');
  showManualDialog.value = false;
  fetchSessions();
}

function openEditDialog(row) {
  Object.assign(editForm, row);
  showEditDialog.value = true;
}

async function submitEditSession() {
  const hall = halls.value.find(h => h.id === editForm.hallId);
  if (!hall) return ElMessage.error('请选择影厅');
  await service.put(`/admin/sessions/${editForm.id}`, editForm);
  ElMessage.success('场次更新成功');
  showEditDialog.value = false;
  fetchSessions();
}

async function deleteSessionRow(id) {
  let res = await service.delete(`/admin/sessions/${id}`);
  let condition = res.error ??false;
  if(!condition)
    ElMessage.success('场次已删除');
  else 
    ElMessage.error('删除失败');
  fetchSessions();
}

// 自动拍片：调用后端自动排片API
async function autoArrangeSessions() {
  if (!todayMovies.value.length) return;
  const movieIds = todayMovies.value.map(m => m.id);
  try {
    const arranged = await service.post('/admin/sessions/auto-arrange', movieIds);
    ElMessage.success(`自动排片完成，安排场次数：${arranged}`);
    fetchSessions();
  } catch (e) {
    ElMessage.error('自动排片失败');
  }
}

// 获取今日所有场次（调用后端API）
async function fetchSessions() {
  try {
    const res = await service.get('/admin/sessions/today');
    // 适配SessionResponseDTO结构
    sessions.value = (res || []).map(item => {
      // 取绝对值id
      const absId = item.id != null ? Math.abs(item.id) : undefined;
      // 根据movieId匹配今日影片
      const movie = todayMovies.value.find(m => m.id === item.movieId);
      return {
        id: absId,
        movieTitle: (movie && movie.title) || '--',
        hallName: (item.hallName || (item.hall && item.hall.name)) || '--',
        startTime: item.startTime || item.start_time || '--',
        endTime: item.endTime || item.end_time || '--',
        price: item.price
      };
    });
  } catch (e) {
    sessions.value = [];
  }
}

async function removeTodayMovie(movieId) {
  try {
    await service.delete(`/admin/movies/today/${movieId}`);
    ElMessage.success('已从今日播映移除');
    todayMovies.value = todayMovies.value.filter(m => m.id !== movieId);
    fetchSessions();
  } catch (e) {
    ElMessage.error('移除失败');
  }
}

onMounted(() => {
  fetchData();
  fetchSessions();
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
.loading-bar {
  text-align: center;
  color: #1890ff;
  margin: 32px 0;
}
.empty-alert {
  margin: 32px 0;
}
.movie-list {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 24px;
}
.movie-item {
  display: flex;
  align-items: center;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 20px;
  gap: 16px;
  min-width: 320px;
}
.movie-poster {
  width: 60px;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
  background: #eee;
}
.movie-info {
  flex: 1;
}
.movie-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}
.movie-meta {
  color: #86909C;
  font-size: 13px;
  margin-bottom: 8px;
}
.session-table {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}
</style> 