<template>
  <div class="movie-browse-view">
    <!-- 顶部导航栏 -->
    <div class="nav-wrapper">
      <el-menu mode="horizontal" :default-active="'2'" class="main-nav container" background-color="#ffffff" text-color="#212121" active-text-color="#FF4081">
        <el-menu-item index="0" class="logo">CinemaBooking</el-menu-item>
        <div class="flex-grow" />
        <el-menu-item index="1" @click="goHome">首页</el-menu-item>
        <el-menu-item index="2">影片浏览</el-menu-item>
        <el-menu-item index="3" v-if="!isLoggedIn" @click="showLogin = true">登录</el-menu-item>
        <el-menu-item index="3" v-else @click="handleLogout">退出</el-menu-item>
        <el-menu-item index="4" @click="showRegister = true">注册</el-menu-item>
      </el-menu>
    </div>
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />

    <!-- 搜索框 -->
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索电影名"
        class="search-input"
        @keyup.enter="onSearch"
        clearable
        @clear="onSearch"
      >
        <template #append>
          <el-button @click="onSearch" icon="el-icon-search">搜索</el-button>
        </template>
      </el-input>
    </div>
    <!-- 分类横向滚动 -->
    <div class="category-section">
      <div class="category-header">影片分类</div>
      <div class="category-tags">
        <span
          v-for="cat in genres"
          :key="cat"
          :class="['category-tag', { active: cat === selectedCategory }]"
          @click="selectCategory(cat)"
        >{{ cat }}</span>
      </div>
    </div>
    <!-- 影片网格 -->
    <div class="movie-grid-container">
      <div class="movie-grid">
        <div v-for="movie in filteredMovies" :key="movie.id" class="movie-card">
          <div class="card-image-wrapper">
            <img :src="movie.poster_url" class="movie-poster" :alt="movie.title" @error="handleImageError"/>
          </div>
          <div class="card-content">
            <div class="movie-title">{{ movie.title }}</div>
            <div class="movie-engagement">
              <span class="engagement-badge">评分 {{ movie.rating ? movie.rating.toFixed(1) : 'N/A' }}</span>
            </div>
            <div class="movie-meta">{{ movie.genres }}</div>
          </div>
        </div>
      </div>
      <!-- 加载更多 -->
      <div class="load-more-container" v-if="!allMoviesLoaded">
        <el-button @click="fetchMovies(true)" :loading="isLoading" class="load-more-btn">
          {{ isLoading ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>
    <footer class="starlight-footer">
      <div class="footer-content">
        © 2024 StarlightCinema ASAKI CINEMA | 专注优质电影票预订服务
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import defaultMoviePng from '../assets/defaultMoviePng.webp';
import { searchMovies } from '../api/movie';

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

const genres = ref(['全部']);
const selectedCategory = ref('全部');
const searchKeyword = ref('');
const movies = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const totalMovies = ref(0);
const isLoading = ref(false);

const allMoviesLoaded = computed(() => movies.value.length >= totalMovies.value && totalMovies.value > 0);

// 本地类型过滤
const filteredMovies = computed(() => {
  if (selectedCategory.value === '全部') return movies.value;
  return movies.value.filter(movie =>
    movie.genres && movie.genres.split(/[ ,，|/]+/).includes(selectedCategory.value)
  );
});

async function fetchMovies(isLoadMore = false) {
  if (isLoading.value) return;
  isLoading.value = true;
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
    };
    const response = await searchMovies(params);
    if (response && response.movies) {
      const newMovies = response.movies.map(movie => ({
        ...movie,
        poster_url: movie.poster_url || defaultMoviePng
      }));
      if (isLoadMore) {
        movies.value = [...movies.value, ...newMovies];
      } else {
        movies.value = newMovies;
      }
      totalMovies.value = response.total;
      currentPage.value++;
      if (response.genres) {
        genres.value = ['全部', ...response.genres.filter(g => g && g !== '全部')];
      }
    }
  } finally {
    isLoading.value = false;
  }
}

function selectCategory(cat) {
  selectedCategory.value = cat;
}

function onSearch() {
  currentPage.value = 1;
  movies.value = [];
  fetchMovies();
}

function handleImageError(event) {
  event.target.src = defaultMoviePng;
}

onMounted(() => {
  fetchMovies();
});
</script>

<style scoped>
.movie-browse-view {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  background: #fff;
  min-height: 100vh;
}
.nav-wrapper {
  border-bottom: 1px solid #e0e0e0;
}
.main-nav {
  border-bottom: none !important;
  height: 64px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
}
.flex-grow {
  flex-grow: 1;
}
.search-section {
  max-width: 600px;
  margin: 32px auto 0 auto;
  display: flex;
  justify-content: center;
}
.search-input {
  width: 100%;
}
.category-section {
  margin: 24px auto 0 auto;
  max-width: 1200px;
  width: 90%;
}
.category-header {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #000;
}
.category-tags {
  display: flex;
  overflow-x: auto;
  gap: 20px;
  padding-bottom: 8px;
  margin-bottom: 32px;
}
.category-tag {
  background: #F0F0F0;
  color: #666;
  font-size: 16px;
  font-weight: 400;
  border-radius: 4px;
  padding: 8px 20px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  white-space: nowrap;
}
.category-tag.active {
  background: #FF4081;
  color: #fff;
}
.movie-grid-container {
  max-width: 1200px;
  margin: 0 auto 40px auto;
  width: 90%;
}
.movie-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 24px;
}
@media (max-width: 1024px) {
  .movie-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 768px) {
  .movie-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.movie-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: pointer;
  position: relative;
  width: 100%;
  min-width: 0;
}
.card-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #f5f5f5;
}
.movie-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0 0 0 0;
}
.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.movie-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #000;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.movie-meta {
  font-size: 16px;
  color: #666;
}
.movie-engagement {
  margin-top: 4px;
}
.engagement-badge {
  background: #FF4081;
  color: #fff;
  font-size: 14px;
  font-weight: 300;
  padding: 4px 8px;
  border-radius: 4px;
}
.starlight-footer {
  width: 100%;
  background: #000;
  color: #CCCCCC;
  font-size: 14px;
  font-weight: 300;
  text-align: center;
  padding: 20px 0 16px 0;
  position: relative;
  margin-top: 40px;
  letter-spacing: 0.5px;
}
.footer-content {
  margin: 0 auto;
  max-width: 90vw;
  line-height: 1.8;
}
@media (max-width: 600px) {
  .starlight-footer {
    font-size: 13px;
    padding: 14px 0 10px 0;
  }
}
.modal-backdrop {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.35);
  z-index: 1000;
  backdrop-filter: blur(2px);
}
.load-more-container {
  text-align: center;
  margin-top: 32px;
}
.load-more-btn {
  padding: 12px 40px;
  font-size: 16px;
  background-color: #FF4081;
  color: white;
  border: none;
}
.load-more-btn:hover {
  background-color: #f50057;
}
</style> 