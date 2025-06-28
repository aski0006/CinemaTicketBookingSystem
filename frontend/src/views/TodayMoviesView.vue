<template>
  <div class="today-movies-root">
    <NavBar
      :is-logged-in="isLoggedIn"
      @show-login="showLogin = true"
      @show-register="showRegister = true"
      @logout="handleLogout"
    />
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />
    <div class="today-movies-flex">
      <div class="semicircle-area" @wheel="onWheel">
        <svg class="semicircle-svg" :width="svgWidth" :height="svgHeight">
          <path :d="semicirclePathD" :stroke="semicircleStroke" :stroke-width="semicircleStrokeWidth" fill="none" />
        </svg>
        <div
          v-for="(movie, i) in movies"
          :key="movie.id"
          class="today-movie-item"
          :style="getMovieItemStyle(i)"
          @click="onMovieClick(i, movie.id)"
        >
          <img :src="movie.poster_url || movie.posterUrl || defaultMoviePng" :alt="movie.title" class="movie-poster" @error="handleImageError" />
        </div>
      </div>
      <div class="main-visual" :style="mainVisualStyle">
        <div class="main-movie-card">
          <img v-if="currentMovie" :src="currentMovie.poster_url || currentMovie.posterUrl || defaultMoviePng" class="main-movie-img" :alt="currentMovie.title" @error="handleImageError" />
        </div>
        <div class="main-buy-btn-right">
          <el-button class="main-buy-btn" @click="goBuyTicket(currentMovie.id)">购票</el-button>
        </div>
      </div>
    </div>
    <teleport to="body">
      <div v-if="showDetail" class="detail-modal">
        <div class="modal-backdrop" @click="closeDetail"></div>
        <div class="modal-content">
          <MovieDetailCard :movie="detailMovie" @close="closeDetail" />
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import MovieDetailCard from '../components/MovieDetailCard.vue';
import defaultMoviePng from '../assets/defaultMoviePng.webp';
import { getTodayMovies } from '../api/movie';
import request from '../api/request';
import { ElMessage } from 'element-plus';
import NavBar from '../components/NavBar.vue';

const router = useRouter();
const showLogin = ref(false);
const showRegister = ref(false);
const isLoggedIn = ref(!!localStorage.getItem('token'));

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
function goHome() {
  window.location.href = '/';
}
function goBrowse() {
  window.location.href = '/movies';
}

const movies = ref([]);
const currentIndex = ref(0);
const showDetail = ref(false);
const detailMovie = ref(null);

const svgWidth = 260;
const svgHeight = window.innerHeight;
const semicircleRadius = Math.min(window.innerHeight * 0.32, window.innerWidth * 0.18);
const semicircleCenter = { x: svgWidth / 2, y: window.innerHeight / 2 };
const semicircleStartAngle = 90;
const semicircleEndAngle = -90;
const semicircleStroke = '#333';
const semicircleStrokeWidth = 1;

const currentMovie = computed(() => movies.value[currentIndex.value] || null);
const mainVisualStyle = computed(() => ({
  background: `url('/city_skyline.jpg') right center/cover no-repeat`
}));

function getAngleByIndex(i) {
  if (movies.value.length <= 1) return 0;
  const total = movies.value.length - 1;
  return semicircleStartAngle + (semicircleEndAngle - semicircleStartAngle) * (i / total);
}
function getMovieItemStyle(i) {
  const angle = getAngleByIndex(i);
  const rad = (angle * Math.PI) / 180;
  const r = semicircleRadius;
  const x = semicircleCenter.x + r * Math.cos(rad) - 90;
  const y = semicircleCenter.y - r * Math.sin(rad) - 120;
  // 透明度和缩放
  const center = currentIndex.value;
  const diff = Math.abs(i - center);
  const maxDiff = Math.max(1, Math.floor(movies.value.length / 2));
  const opacity = 1 - 0.8 * (diff / maxDiff);
  const scale = 1 - 0.15 * (diff / maxDiff);
  return {
    position: 'absolute',
    left: `${x}px`,
    top: `${y}px`,
    width: '180px',
    height: '240px',
    zIndex: 10 + (100 - diff),
    opacity: opacity,
    transform: `scale(${scale}) rotate(${-angle}deg)`,
    transition: 'all 0.7s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: i === center ? '0 8px 32px rgba(0,0,0,0.15)' : '0 2px 8px rgba(0,0,0,0.08)',
    cursor: i === center ? 'default' : 'pointer',
    filter: i === center ? 'brightness(1)' : 'brightness(0.85)',
    willChange: 'transform, opacity',
    transformStyle: 'preserve-3d',
    border: i === center ? '2px solid #1890ff' : 'none',
    borderRadius: '12px',
    background: '#fff',
    perspective: '1200px'
  };
}
const semicirclePathD = computed(() => {
  // SVG弧形路径
  const r = semicircleRadius;
  const x0 = semicircleCenter.x + r * Math.cos((semicircleStartAngle * Math.PI) / 180);
  const y0 = semicircleCenter.y - r * Math.sin((semicircleStartAngle * Math.PI) / 180);
  const x1 = semicircleCenter.x + r * Math.cos((semicircleEndAngle * Math.PI) / 180);
  const y1 = semicircleCenter.y - r * Math.sin((semicircleEndAngle * Math.PI) / 180);
  return `M${x0},${y0} A${r},${r} 0 0,1 ${x1},${y1}`;
});

function selectMovie(i) {
  currentIndex.value = i;
}
function prevMovie() {
  if (currentIndex.value > 0) currentIndex.value--;
}
function nextMovie() {
  if (currentIndex.value < movies.value.length - 1) currentIndex.value++;
}
function onWheel(e) {
  if (e.deltaY > 0) {
    nextMovie();
  } else if (e.deltaY < 0) {
    prevMovie();
  }
}
function handleImageError(event) {
  event.target.src = defaultMoviePng;
}
// 详情弹窗相关
async function onMovieClick(i, movieId) {
  selectMovie(i);
  try {
    const res = await request.get(`/api/movies/${movieId}`);
    detailMovie.value = res;
    showDetail.value = true;
  } catch (e) {
    alert('获取影片详情失败');
  }
}
function closeDetail() {
  showDetail.value = false;
  detailMovie.value = null;
}
function goBuyTicket(movieId) {
  router.push({
    name: 'TicketBooking',
    params: { movieId: movieId }
  });
}

async function fetchTodayMovies() {
  const res = await getTodayMovies({ page: 1, size: 100 });
  if (res && res.movies) {
    movies.value = res.movies.map(m => ({
      ...m,
      poster_url: m.poster_url || m.posterUrl || defaultMoviePng
    }));
    if (currentIndex.value >= movies.value.length) currentIndex.value = 0;
  }
}
onMounted(() => {
  fetchTodayMovies();
});
</script>

<style scoped>
.today-movies-root {
  width: 100vw;
  height: 100vh;
  min-height: 600px;
  background: #f8f8f8;
  overflow: hidden;
  position: relative;
  perspective: 1200px;
}
.today-movies-flex {
  display: flex;
  flex-direction: row;
  width: 100vw;
  height: 100vh;
  min-height: 600px;
}
.semicircle-area {
  position: relative;
  width: 260px;
  min-width: 180px;
  height: 100vh;
  background: transparent;
  overflow: visible;
  padding-top: 60px;
}
.semicircle-svg {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  width: 260px;
  height: 100vh;
  pointer-events: none;
}
.today-movie-item {
  will-change: transform, opacity;
  transform-style: preserve-3d;
  user-select: none;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.today-movie-item .movie-poster {
  width: 160px;
  height: 210px;
  object-fit: cover;
  border-radius: 10px;
  background: #f5f5f5;
  box-shadow: 0 2px 12px rgba(255,64,129,0.12);
}
.main-visual {
  flex: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-position: right center;
  background-size: cover;
  background-repeat: no-repeat;
  position: relative;
}
.main-movie-card {
  width: 400px;
  height: 540px;
  background: rgba(255,255,255,0.98);
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}
.main-movie-img {
  width: 360px;
  height: 500px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(24,144,255,0.10);
}
.main-buy-btn-right {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  z-index: 10;
  padding-right: 32px;
}
.main-buy-btn {
  width: 120px;
  height: 80px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 2px;
  border-radius: 22px;
  background: linear-gradient(135deg, #ff3576 0%, #ff8c1a 100%);
  color: #fff;
  margin-top: 0;
  writing-mode: vertical-rl;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  box-shadow: 0 4px 18px rgba(255,64,129,0.18);
  transition: background 0.2s, box-shadow 0.2s, transform 0.15s;
  cursor: pointer;
}
.main-buy-btn:hover {
  background: linear-gradient(135deg, #ff1744 0%, #ffb300 100%);
  box-shadow: 0 8px 32px rgba(255,64,129,0.28);
  transform: scale(1.06);
}
@media (max-width: 900px) {
  .today-movies-flex {
    flex-direction: column;
  }
  .semicircle-area {
    width: 100vw;
    min-width: 0;
    height: 40vh;
  }
  .main-visual {
    height: 60vh;
  }
  .main-movie-card {
    width: 90vw;
    height: 60vw;
  }
  .main-movie-img {
    width: 80vw;
    height: 50vw;
  }
}
.pink-title {
  color: #FF4081 !important;
  text-shadow: 0 2px 12px rgba(255,64,129,0.10);
}
/* 详情弹窗样式 */
.detail-modal {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-backdrop {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.35);
  z-index: 1999;
}
.modal-content {
  position: relative;
  z-index: 2001;
}
</style> 