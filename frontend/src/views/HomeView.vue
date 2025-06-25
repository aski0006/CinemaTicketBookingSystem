<template>
  <div class="home-view">
    <!-- Navigation -->
    <div class="nav-wrapper">
      <el-menu mode="horizontal" :default-active="activeIndex" class="main-nav container" background-color="#ffffff" text-color="#212121" active-text-color="#1e88e5">
        <el-menu-item index="0" class="logo">CinemaBooking</el-menu-item>
        <div class="flex-grow" />
        <el-menu-item index="1">首页</el-menu-item>
        <el-menu-item index="2">影院</el-menu-item>
        <el-menu-item index="3">影片浏览</el-menu-item>
        <el-menu-item index="4" @click="showLogin = true">登录</el-menu-item>
        <el-menu-item index="5" @click="showRegister = true">注册</el-menu-item>
      </el-menu>
    </div>
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />

    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content container">
        <h1 class="hero-title">ASAKI CINEMA</h1>
        <p class="hero-subtitle">快来看看这些不容错过的热门影片，开启你的观影之旅！</p>
        <el-button class="hero-cta" type="primary" size="large" round>{{ ctaButtonText }}</el-button>
      </div>
    </div>

    <!-- Recommendation Module -->
    <el-main class="recommendation-module">
      <div class="container">
        <h2 class="section-title">精彩影片推荐</h2>
        <el-carousel height="340px" indicator-position="outside" arrow="hover" class="movie-carousel" @change="onCarouselChange">
          <el-carousel-item v-for="movie in recommendedMovies" :key="movie.id">
            <img
              :src="movie.poster_url"
              class="carousel-movie-poster"
              :alt="movie.title"
              @click="() => handleMovieClick(movie)"
            />
          </el-carousel-item>
        </el-carousel>
        <div class="carousel-movie-info">
          <span class="movie-title">{{ currentMovie.title }}</span>
          <el-rate
            v-model="currentMovie.rating"
            disabled
            show-score
            text-color="#ff9900"
            score-template="{value} 分"
          />
        </div>
      </div>
    </el-main>
    <!-- Feature Showcase Section -->
    <section class="feature-showcase-section">
      <div class="container feature-showcase-grid">
        <div class="feature-item" v-for="(feature, idx) in features" :key="idx">
          <img :src="feature.img" class="feature-img" :alt="feature.title" />
          <div class="feature-title">{{ feature.title }}</div>
          <div class="feature-desc">{{ feature.desc }}</div>
        </div>
      </div>
    </section>

    <!-- Testimonials Section -->
    <section class="testimonials-section">
      <div class="container testimonials-grid">
        <div class="testimonial-card" v-for="(t, idx) in testimonials" :key="idx">
          <img :src="t.avatar" class="testimonial-avatar" :alt="t.name" />
          <div class="testimonial-name">{{ t.name }}</div>
          <div class="testimonial-title">{{ t.title }}</div>
          <div class="testimonial-text">“{{ t.text }}”</div>
        </div>
      </div>
    </section>

    <!-- Pricing Section -->
    <section class="pricing-section">
      <div class="container">
        <h2 class="section-title">成为会员，畅享特权</h2>
        <el-radio-group v-model="pricingDuration" size="large" class="duration-switcher">
          <el-radio-button label="monthly">包月</el-radio-button>
          <el-radio-button label="quarterly">包季</el-radio-button>
          <el-radio-button label="yearly">包年</el-radio-button>
        </el-radio-group>

        <div class="pricing-cards" @mouseleave="handlePricingMouseLeave">
          <div
            v-for="(plan, idx) in plans"
            :key="plan.name"
            :class="['pricing-card', { active: activePricingIndex === idx }]"
            @mouseenter="handlePricingMouseEnter(idx)"
          >
            <h3 class="plan-name">{{ plan.name }}</h3>
            <div class="plan-price">
              <span class="price-currency">¥</span>
              <span class="price-amount">{{ plan.prices[pricingDuration] }}</span>
              <span class="price-duration">/ {{ durationText }}</span>
            </div>
            <el-button :type="activePricingIndex === idx ? 'primary' : 'default'" class="buy-button" round plain>{{ plan.buttonText }}</el-button>
            <el-divider />
            <ul class="plan-features">
              <li v-for="feature in plan.features" :key="feature">{{ feature }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <el-footer class="footer">
      <div class="container">
        <p>&copy; 2024 CinemaBooking Visual Language. All rights reserved.</p>
      </div>
    </el-footer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import defaultMoviePng from '../assets/defaultMoviePng.webp'
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
const activeIndex = ref('1');
const ctaButtonText = ref('立即购票');
const recommendedMovies = ref([
  { id: 1, title: '沙丘', poster_url: defaultMoviePng, rating: 4.5 },
  { id: 2, title: '星际穿越', poster_url: defaultMoviePng, rating: 4.9 },
  { id: 3, title: '盗梦空间', poster_url: defaultMoviePng, rating: 4.8 },
  { id: 4, title: '瞬息全宇宙', poster_url: defaultMoviePng, rating: 4.7 },
  { id: 5, title: '奥本海默', poster_url: defaultMoviePng, rating: 4.6 },
]);

const carouselIndex = ref(0);
const currentMovie = computed(() => recommendedMovies.value[carouselIndex.value] || {});

function onCarouselChange(idx) {
  carouselIndex.value = idx;
}

function handleMovieClick(movie) {
  // 预留：跳转到影片详情页
  // 例如：router.push(`/movie/${movie.id}`)
  console.log('点击了影片', movie);
}

const pricingDuration = ref('monthly'); // 'monthly', 'quarterly', 'yearly'
const hoverIndex = ref(null);

const plans = ref([
  {
    name: '基础会员',
    prices: { monthly: 0, quarterly: 0, yearly: 0 },
    features: [
      '在线选座',
      '查看影片信息',
      '发表电影评论',
      '——',
      '——'
    ],
    buttonType: 'default',
    buttonText: '立即注册'
  },
  {
    name: 'VIP 会员',
    prices: { monthly: 25, quarterly: 68, yearly: 248 },
    features: [
      '基础会员所有权益',
      '购票享9折优惠',
      '每月赠送2张观影券',
      '优先参与点映活动',
      '——'
    ],
    buttonType: 'primary',
    buttonText: '立即购买'
  },
  {
    name: 'SVIP 会员',
    prices: { monthly: 99, quarterly: 288, yearly: 998 },
    features: [
      'VIP会员所有权益',
      '购票享8折优惠',
      '每月赠送5张观影券',
      '无限次免费退改签',
      '专享线下见面会'
    ],
    buttonType: 'default',
    buttonText: '立即购买'
  }
]);

const activePricingIndex = computed(() => {
  if (hoverIndex.value !== null) return hoverIndex.value;
  return 1; // 默认高亮VIP
});

function handlePricingMouseEnter(idx) {
  hoverIndex.value = idx;
}
function handlePricingMouseLeave() {
  hoverIndex.value = null;
}

const durationText = computed(() => {
  switch (pricingDuration.value) {
    case 'monthly': return '月';
    case 'quarterly': return '季';
    case 'yearly': return '年';
    default: return '';
  }
});

// 功能亮点数据
const features = [
  {
    img: 'https://images.unsplash.com/photo-1517602302552-471fe67acf66?auto=format&fit=crop&w=400&q=80',
    title: '自由选座',
    desc: '自由选择心仪座位，打造专属观影体验。'
  },
  {
    img: 'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?auto=format&fit=crop&w=400&q=80',
    title: '多样影片',
    desc: '海量热门影片，满足不同观影口味。'
  },
  {
    img: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=400&q=80',
    title: '便捷支付',
    desc: '多种支付方式，购票安全又高效。'
  }
];
// 用户评价数据
const testimonials = [
  {
    avatar: 'https://randomuser.me/api/portraits/men/32.jpg',
    name: '小明',
    title: '观影达人',
    text: '购票流程很顺畅，选座自由，体验感满分！'
  },
  {
    avatar: 'https://randomuser.me/api/portraits/women/44.jpg',
    name: '小红',
    title: '电影爱好者',
    text: '影片种类丰富，界面也很有胶片氛围，喜欢！'
  },
  {
    avatar: 'https://randomuser.me/api/portraits/men/65.jpg',
    name: '老王',
    title: '资深影迷',
    text: '会员购票优惠多，客服响应快，值得推荐。'
  }
];

const showLogin = ref(false)
const showRegister = ref(false)
function handleSwitchDialog(type) {
  if (type === 'login') {
    showRegister.value = false
    showLogin.value = true
  } else {
    showLogin.value = false
    showRegister.value = true
  }
}
</script>

<style scoped>
.container {
  width: 90%;
  margin: 0 auto;
}

.home-view {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  background-color: #ffffff;
  text-align: left;
  min-height: 100vh;
  background: url('https://images.unsplash.com/photo-1465101178521-c1a9136a3b99?auto=format&fit=crop&w=1200&q=80') center/cover no-repeat fixed, #ffffff;
}

.nav-wrapper {
  border-bottom: 1px solid #e0e0e0;
}

.main-nav {
  border-bottom: none !important;
  height: 60px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.flex-grow {
  flex-grow: 1;
}

.hero-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 380px;
  padding: 40px 24px;
  background: #121212;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  left: 0; top: 0; right: 0; bottom: 0;
  z-index: 0;
  background:
    linear-gradient(120deg, rgba(18,18,18,0.7) 60%, rgba(18,18,18,0.95) 100%),
    url('https://images.unsplash.com/photo-1464983953574-0892a716854b?auto=format&fit=crop&w=800&q=80') center/cover no-repeat;
  /* film-grain纹理 */
  pointer-events: none;
}

.hero-bg::after {
  content: '';
  position: absolute;
  left: 0; top: 0; right: 0; bottom: 0;
  background: url('https://www.transparenttextures.com/patterns/grain.png') repeat;
  opacity: 0.25;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 480px;
  margin: 0 auto;
  padding: 48px 32px 40px 32px;
  border-radius: 24px;
  background: rgba(18,18,18,0.7);
  box-shadow: 0 8px 32px rgba(30,136,229,0.10);
}

.hero-title {
  font-family: sans-serif;
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 18px;
  letter-spacing: 1px;
}

.hero-subtitle {
  font-family: sans-serif;
  font-size: 16px;
  font-weight: normal;
  color: #cccccc;
  line-height: 1.4;
  margin-bottom: 32px;
}

.hero-cta {
  font-family: sans-serif;
  font-size: 18px;
  font-weight: bold;
  background: #1e88e5;
  color: #fff;
  border-radius: 4px;
  padding: 12px 32px;
  border: none;
  letter-spacing: 1px;
}

.hero-cta:hover {
  background: #1565c0;
  color: #fff;
}

.recommendation-module {
  padding: 80px 0;
  background: #fff;
}

.section-title {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: #1e88e5;
  margin-bottom: 40px;
}

.movie-carousel {
  width: 70%;
  min-width: 320px;
  max-width: 700px;
  margin: 0 auto;
}

.carousel-movie-poster {
  width: 100%;
  height: 340px;
  object-fit: cover;
  background: #f5f5f5;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(30,136,229,0.10);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.carousel-movie-poster:hover {
  box-shadow: 0 8px 32px rgba(30,136,229,0.18);
}

.carousel-movie-info {
  text-align: center;
  margin-top: 24px;
}

.movie-title {
  font-size: 16px;
  font-weight: 500;
  color: #212121;
}

.bottom {
    margin-top: 10px;
}

.footer {
  padding: 20px 0;
  margin-top: 40px;
  border-top: 1px solid #eeeeee;
  color: #757575;
  background-color: #f8f9fa;
}

.footer .container {
    text-align: center;
}

/* Pricing Section */
.pricing-section {
  padding: 80px 0;
  background: #fff;
}

.pricing-section > .container {
  padding: 60px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 123, 255, 0.1);
  box-sizing: border-box;
}

.pricing-section .section-title {
  margin-top: 0; /* Adjust title margin for the new padded container */
}

.duration-switcher {
  display: flex;
  justify-content: center;
  margin-bottom: 40px;
}

.pricing-cards {
  display: flex;
  justify-content: center;
  gap: 30px;
  flex-wrap: wrap;
  align-items: center;
}

.pricing-card {
  background-color: #ffffff;
  border: 1.5px solid #e0e0e0;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  width: 300px;
  transition: transform 0.3s, box-shadow 0.3s, background 0.3s, color 0.3s, border-color 0.3s;
  cursor: pointer;
}

.pricing-card.active,
.pricing-card.recommended {
  background-color: #1e88e5;
  color: #212121;
  border-color: #1e88e5;
  transform: scale(1.05);
  z-index: 2;
}

.pricing-card:hover {
  background-color: #1565c0;
  color: #fff;
  border-color: #1565c0;
  transform: scale(1.05);
  z-index: 2;
}

.pricing-card.active .plan-features li,
.pricing-card.active .plan-features li::before {
  color: #212121;
}

.plan-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.plan-price {
  margin-bottom: 30px;
}

.price-currency {
  font-size: 24px;
  vertical-align: super;
  margin-right: 4px;
}

.price-amount {
  font-size: 48px;
  font-weight: bold;
}

.price-duration {
  font-size: 16px;
  color: #757575;
}

.pricing-card.recommended .price-duration {
  color: #eeeeee;
}

.buy-button {
  width: 100%;
  font-size: 16px;
  font-weight: bold;
  padding: 18px 0;
}

.pricing-card.recommended .buy-button {
    background: #ffffff;
    color: #1e88e5;
    border-color: #ffffff;
}

.pricing-card.recommended .buy-button:hover {
    background: #f0f0f0;
}

.plan-features {
  list-style: none;
  padding: 0;
  margin-top: 30px;
  text-align: left;
}

.plan-features li {
  margin-bottom: 15px;
  color: #757575;
  display: flex;
  align-items: center;
}

.plan-features li::before {
  content: '✓';
  color: #1e88e5;
  margin-right: 10px;
  font-weight: bold;
}

.pricing-card.recommended .plan-features li {
  color: #ffffff;
}

.pricing-card.recommended .plan-features li::before {
  color: #ffffff;
}

/* Feature Showcase Section */
.feature-showcase-section {
  padding: 80px 0 40px 0;
  background: #fff;
}
.feature-showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  justify-items: center;
  align-items: start;
}
.feature-item {
  text-align: center;
  max-width: 320px;
}
.feature-img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.feature-title {
  font-size: 18px;
  font-weight: bold;
  margin: 10px 0 5px 0;
  color: #000;
}
.feature-desc {
  font-size: 14px;
  color: #444;
}

/* Testimonials Section */
.testimonials-section {
  padding: 40px 0 80px 0;
  background: #fff;
}
.testimonials-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  justify-items: center;
}
.testimonial-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(30,136,229,0.08);
  padding: 20px;
  max-width: 300px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.testimonial-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 10px;
}
.testimonial-name {
  font-size: 16px;
  font-weight: 600;
  margin: 10px 0 5px 0;
  color: #000;
}
.testimonial-title {
  font-size: 12px;
  color: #666;
}
.testimonial-text {
  font-size: 14px;
  font-style: italic;
  color: #222;
  padding: 15px 0 0 0;
}
@media (max-width: 900px) {
  .feature-showcase-grid, .testimonials-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

.modal-backdrop {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.35);
  z-index: 1000;
  backdrop-filter: blur(2px);
}
</style> 