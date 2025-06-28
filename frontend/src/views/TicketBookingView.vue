<template>
  <div class="ticket-booking-root">
    <NavBar
      :is-logged-in="isLoggedIn"
      @show-login="showLogin = true"
      @show-register="showRegister = true"
      @logout="handleLogout"
    />
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />
    
    <div class="booking-container">
      <div class="left-panel">
        <MovieDetailCard :movie="movie" @close="goBack" :close-button-text="'返回'" />
      </div>
      <div class="right-panel">
        <div class="sessions-header">
          <h2>选择场次</h2>
          <el-button @click="goBack" type="text">返回</el-button>
        </div>
        
        <div v-if="movie" class="movie-title">
          <h3>{{ movie.title }}</h3>
        </div>
        
        <div v-if="loading" class="loading-container">
          <el-loading-spinner />
          <p>加载场次信息中...</p>
        </div>
        
        <div v-else-if="sessions.length === 0" class="empty-sessions">
          <el-empty description="暂无场次信息" />
        </div>
        
        <div v-else class="sessions-list">
          <div 
            v-for="session in sessions" 
            :key="session.id" 
            class="session-item"
            :class="{ 
              'selected': selectedSession?.id === session.id,
              'disabled': session.status === 'FULL' || session.availableSeats === 0
            }"
            @click="selectSession(session)"
          >
            <div class="session-time">
              <div class="time">{{ formatTime(session.startTime) }}</div>
              <div class="duration">{{ session.movieDuration }}分钟</div>
            </div>
            <div class="session-info">
              <div class="hall-name">{{ session.hallName }}</div>
              <div class="price">¥{{ session.price }}</div>
              <div class="seats-info">剩余座位：{{ session.availableSeats }}/{{ session.totalSeats }}</div>
            </div>
            <div class="session-status">
              <el-tag 
                :type="getSessionStatusType(session.status)" 
                size="small"
              >
                {{ getSessionStatusText(session.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div v-if="selectedSession" class="booking-actions">
          <el-button 
            type="primary" 
            size="large" 
            @click="proceedToSeatSelection"
            :disabled="!selectedSession"
          >
            选择座位
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import NavBar from '../components/NavBar.vue';
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import MovieDetailCard from '../components/MovieDetailCard.vue';
import { getMovieById } from '../api/movie';
import { getTodaySessions } from '../api/session';

const route = useRoute();
const router = useRouter();

const showLogin = ref(false);
const showRegister = ref(false);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const loading = ref(true);
const movie = ref(null);
const sessions = ref([]);
const selectedSession = ref(null);

function handleSwitchDialog(type) {
  if (type === 'login') {
    showRegister.value = false;
    showLogin.value = true;
  } else {
    showLogin.value = false;
    showRegister.value = true;
  }
}

function handleLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('member_level');
  isLoggedIn.value = false;
  showLogin.value = false;
}

function goBack() {
  router.go(-1);
}

function selectSession(session) {
  if (session.status === 'FULL' || session.availableSeats === 0) {
    ElMessage.warning('该场次已满座');
    return;
  }
  selectedSession.value = session;
}

function formatTime(timeStr) {
  if (!timeStr) return '--';
  // 如果是LocalDateTime对象，提取时间部分
  if (typeof timeStr === 'string') {
    // 如果是ISO格式的时间字符串，提取HH:mm部分
    if (timeStr.includes('T')) {
      return timeStr.substring(11, 16); // 提取 HH:mm
    }
    return timeStr.substring(0, 5); // 只显示 HH:mm
  }
  return '--';
}

function getSessionStatusType(status) {
  switch (status) {
    case 'AVAILABLE': return 'success';
    case 'FULL': return 'warning';
    case 'CANCELLED': return 'danger';
    default: return 'info';
  }
}

function getSessionStatusText(status) {
  switch (status) {
    case 'AVAILABLE': return '可预订';
    case 'FULL': return '已满座';
    case 'CANCELLED': return '已取消';
    default: return '未知';
  }
}

function proceedToSeatSelection() {
  if (!selectedSession.value) {
    ElMessage.warning('请先选择场次');
    return;
  }
  
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录');
    showLogin.value = true;
    return;
  }
  
  // 跳转到选座页面
  router.push({
    name: 'SeatSelection',
    params: { 
      movieId: movie.value.id,
      sessionId: selectedSession.value.id 
    }
  });
}

async function fetchMovieData() {
  try {
    const movieId = route.params.movieId;
    if (!movieId) {
      ElMessage.error('影片ID不存在');
      goBack();
      return;
    }
    
    // 获取影片详情
    const movieData = await getMovieById(movieId);
    if (!movieData) {
      ElMessage.error('影片不存在');
      goBack();
      return;
    }
    movie.value = movieData;
    
    // 获取场次信息
    const sessionsData = await getTodaySessions(movieId);
    // 处理场次数据，适配后端返回的结构
    sessions.value = (sessionsData || []).map(session => ({
      id: session.id,
      startTime: session.startTime || session.start_time,
      endTime: session.endTime || session.end_time,
      price: session.price,
      hallName: session.hallName,
      movieDuration: movieData.duration || 120,
      status: session.status || 'AVAILABLE',
      availableSeats: session.availableSeats || 0,
      totalSeats: session.totalSeats || 0,
      seatLayout: session.seatLayout || ''
    }));
    
  } catch (error) {
    console.error('获取数据失败:', error);
    ElMessage.error('获取数据失败，请稍后重试');
    goBack();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchMovieData();
});
</script>

<style scoped>
.ticket-booking-root {
  min-height: 100vh;
  background: #f5f7fa;
}

.booking-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  gap: 30px;
  min-height: calc(100vh - 80px);
}

.left-panel {
  flex: 1;
  max-width: 500px;
  display: flex;
  align-items: flex-start;
}

.left-panel .movie-detail-card {
  margin: 0;
  max-width: none;
  width: 100%;
}

.right-panel {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.sessions-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.movie-title {
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.movie-title h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #666;
}

.empty-sessions {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 30px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.session-item:hover {
  border-color: #1890ff;
  background: #f0f8ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
}

.session-item.selected {
  border-color: #1890ff;
  background: #e6f7ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.session-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f5f5;
}

.session-item.disabled:hover {
  border-color: #f0f0f0;
  background: #f5f5f5;
  transform: none;
  box-shadow: none;
}

.session-time {
  flex: 1;
  text-align: center;
}

.time {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.duration {
  font-size: 12px;
  color: #666;
}

.session-info {
  flex: 2;
  text-align: center;
}

.hall-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.price {
  font-size: 18px;
  font-weight: 600;
  color: #ff4d4f;
}

.seats-info {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.session-status {
  flex: 1;
  text-align: center;
}

.booking-actions {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
}

.booking-actions .el-button {
  padding: 12px 40px;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .booking-container {
    flex-direction: column;
    padding: 10px;
  }
  
  .left-panel {
    max-width: none;
  }
  
  .right-panel {
    padding: 20px;
  }
  
  .session-item {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
  
  .session-time,
  .session-info,
  .session-status {
    flex: none;
  }
}
</style> 