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
          <span class="page-title">电影管理</span>
          <el-button type="primary" @click="openAddDialog">新增电影</el-button>
          <el-button type="success" @click="setTodayMovies" :disabled="!selectedMovieIds.length">今日播映</el-button>
          <el-input v-model="searchKeyword" placeholder="搜索电影标题" class="search-input" clearable @input="fetchMovies(1)" />
        </div>
        <el-table :data="movies" border stripe size="small" class="movie-table" @selection-change="handleSelectionChange" :row-class-name="rowClassName" ref="movieTable" :highlight-current-row="false">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="director" label="导演" />
          <el-table-column prop="actors" label="主演" />
          <el-table-column prop="duration" label="时长(分钟)" width="100" />
          <el-table-column prop="release_date" label="上映日期" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteMovie(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="fetchMovies"
          />
        </div>
        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="showDialog" :title="dialogMode === 'add' ? '新增电影' : '编辑电影'" width="500px">
          <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" />
            </el-form-item>
            <el-form-item label="导演" prop="director">
              <el-input v-model="form.director" />
            </el-form-item>
            <el-form-item label="主演" prop="actors">
              <el-input v-model="form.actors" />
            </el-form-item>
            <el-form-item label="时长" prop="duration">
              <el-input-number v-model="form.duration" :min="1" />
            </el-form-item>
            <el-form-item label="上映日期" prop="release_date">
              <el-date-picker v-model="form.release_date" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status">
                <el-option label="即将上映" value="UPCOMING" />
                <el-option label="热映中" value="SHOWING" />
                <el-option label="已下架" value="OFF" />
              </el-select>
            </el-form-item>
            <el-form-item label="海报URL" prop="poster_url">
              <el-input v-model="form.poster_url" />
            </el-form-item>
            <el-form-item label="简介" prop="description">
              <el-input v-model="form.description" type="textarea" rows="3" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showDialog = false">取消</el-button>
            <el-button type="primary" @click="submitForm">保存</el-button>
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
import request from '@/api/request';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { useRouter } from 'vue-router';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';

const movies = ref([]);
const total = ref(0);
const pageSize = ref(10);
const currentPage = ref(1);
const searchKeyword = ref('');

const showDialog = ref(false);
const dialogMode = ref('add'); // 'add' or 'edit'
const form = reactive({
  id: null,
  title: '',
  director: '',
  actors: '',
  duration: 90,
  release_date: '',
  status: 'UPCOMING',
  poster_url: '',
  description: ''
});
const formRef = ref();
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  director: [{ required: true, message: '请输入导演', trigger: 'blur' }],
  duration: [{ required: true, type: 'number', message: '请输入时长', trigger: 'blur' }],
  release_date: [{ required: true, message: '请选择上映日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};


const activeSidebar = ref('电影管理');
const sidebarCollapsed = ref(false);
const router = useRouter();

const selectedMovieIds = ref([]);
function handleSelectionChange(selection) {
  selectedMovieIds.value = selection.map(row => row.id);
}
async function setTodayMovies() {
  if (!selectedMovieIds.value.length) return;
  // 只允许状态为SHOWING的影片
  const showingIds = movies.value.filter(m => selectedMovieIds.value.includes(m.id) && m.status === 'SHOWING').map(m => m.id);
  const offIds = movies.value.filter(m => selectedMovieIds.value.includes(m.id) && m.status === 'OFF').map(m => m.title);
  if (offIds.length > 0) {
    ElMessage.error(`下架影片无法设置为今日播映: ${offIds.join('、')}`);
    return;
  }
  if (showingIds.length === 0) {
    ElMessage.warning('请选择"热映中"状态的影片进行今日播映');
    return;
  }
  await request.post('/admin/movies/today', showingIds);
  ElMessage.success('已设置今日播映影片');
}
function rowClassName({ row }) {
  return selectedMovieIds.value.includes(row.id) ? 'selected-row' : '';
}

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

async function fetchMovies(page = 1) {
  currentPage.value = page;
  const params = { page, size: pageSize.value, keyword: searchKeyword.value };
  const res = await request.get('/admin/movies', { params });
  movies.value = res.movies || [];
  total.value = Number(res.total) || 0;
}

function openAddDialog() {
  dialogMode.value = 'add';
  Object.assign(form, { id: null, title: '', director: '', actors: '', duration: 90, release_date: '', status: 'UPCOMING', poster_url: '', description: '' });
  showDialog.value = true;
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  Object.assign(form, row);
  showDialog.value = true;
}

async function submitForm() {
  await formRef.value.validate();
  // 构造后端需要的字段
  const payload = {
    title: form.title,
    director: form.director,
    actors: form.actors,
    duration: form.duration,
    releaseDate: form.release_date, // 转换字段名
    status: form.status,
    posterUrl: form.poster_url,
    description: form.description
  };
  if (dialogMode.value === 'add') {
    await request.post('/admin/movies', payload);
    ElMessage.success('新增成功');
  } else {
    await request.put(`/admin/movies/${form.id}`, payload);
    ElMessage.success('修改成功');
  }
  showDialog.value = false;
  fetchMovies(currentPage.value);
}

async function deleteMovie(id) {
  await request.delete(`/admin/movies/${id}`);
  ElMessage.success('删除成功');
  fetchMovies(currentPage.value);
}

onMounted(() => {
  fetchMovies();
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
.search-input {
  width: 240px;
  margin-left: auto;
}
.movie-table {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 24px;
}
.selected-row {
  background: #e8f3ff !important;
}
</style>