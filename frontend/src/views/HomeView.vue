<template>
  <div class="home-view">
    <!-- Navigation -->
    <div class="nav-wrapper">
      <el-menu mode="horizontal" :default-active="activeIndex" class="main-nav container" background-color="#ffffff" text-color="#212121" active-text-color="#1e88e5">
        <el-menu-item index="0" class="logo">CinemaBooking</el-menu-item>
        <div class="flex-grow" />
        <el-menu-item index="1">首页</el-menu-item>
        <el-menu-item index="2" @click="goMovieBrowse">影片浏览</el-menu-item>
        <el-menu-item index="3" v-if="!isLoggedIn" @click="showLogin = true">登录</el-menu-item>
        <el-menu-item index="3" v-else @click="handleLogout">退出</el-menu-item>
        <el-menu-item index="4" @click="showRegister = true">注册</el-menu-item>
      </el-menu>
    </div>
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="handleLoginDialogClose" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />

    <!-- Hero Section，背景图片+底部渐变 -->
    <div class="hero-section-bg">
      <div class="hero-section">
        <div class="hero-bg"></div>
        <div class="hero-content container">
          <h1 class="hero-title">ASAKI CINEMA</h1>
          <p class="hero-subtitle">快来看看这些不容错过的热门影片，开启你的观影之旅！</p>
          <el-button v-if="!isLoggedIn" class="hero-cta" type="primary" size="large" round @click="showLogin = true">{{ ctaButtonText }}</el-button>
          <div v-else class="hero-welcome">欢迎回来，祝您观影愉快！</div>
        </div>
        <div class="hero-fade-to-card"></div>
      </div>
    </div>

    <!-- 推荐影片 卡片 -->
    <el-card class="section-card recommend-card" shadow="always">
      <div class="recommendation-module card-content">
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
      </div>
    </el-card>

    <!-- 功能亮点 卡片 -->
    <el-card class="section-card feature-card" shadow="always">
      <section class="feature-showcase-section card-content">
        <div class="container feature-showcase-grid">
          <div class="feature-item" v-for="(feature, idx) in features" :key="idx">
            <img :src="feature.img" class="feature-img" :alt="feature.title" />
            <div class="feature-title">{{ feature.title }}</div>
            <div class="feature-desc">{{ feature.desc }}</div>
          </div>
        </div>
      </section>
    </el-card>

    <!-- 用户评价 卡片 -->
    <el-card class="section-card testimonial-card" shadow="always">
      <section class="testimonials-section card-content">
        <div class="container testimonials-grid">
          <div class="testimonial-card" v-for="(t, idx) in testimonials" :key="idx">
            <img :src="t.avatar" class="testimonial-avatar" :alt="t.name" />
            <div class="testimonial-name">{{ t.name }}</div>
            <div class="testimonial-title">{{ t.title }}</div>
            <div class="testimonial-text">“{{ t.text }}”</div>
          </div>
        </div>
      </section>
    </el-card>

    <!-- 会员购模块（无卡片，白色背景） -->
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
            :class="['pricing-card', { active: activePricingIndex === idx, owned: isLoggedIn && memberLevel === idx }]"
            @mouseenter="(e) => handlePricingMouseEnter(idx, e)"
            @mousemove="(e) => handleOwnedCardMouseMove(idx, e)"
            @mouseleave="(e) => { handlePricingMouseLeave(); handleOwnedCardMouseLeave(idx, e) }"
          >
            <h3 class="plan-name">{{ plan.name }}</h3>
            <div class="plan-price">
              <span class="price-currency">¥</span>
              <span class="price-amount">{{ plan.prices[pricingDuration] }}</span>
              <span class="price-duration">/ {{ durationText }}</span>
            </div>
            <el-button
              :type="activePricingIndex === idx ? 'primary' : 'default'"
              class="buy-button"
              round plain
              @click="handleBuyClick(plan)"
              :disabled="isPlanButtonDisabled(plan, idx)"
              :loading="buyLoading && (plan.name.includes('VIP') || plan.name.includes('SVIP'))"
            >{{ getPlanButtonText(plan, idx) }}</el-button>
            <el-divider />
            <ul class="plan-features">
              <li v-for="feature in plan.features" :key="feature">{{ feature }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ模块 -->
    <el-card class="section-card faq-card" shadow="always">
      <section class="faq-section card-content">
        <h1 class="faq-title">常见问题解答</h1>
        <div class="faq-intro">欢迎来到影院订票桌面网站，涵盖多种订票功能，为您提供便捷服务！</div>
        <div class="faq-list">
          <div class="faq-entry" id="faq1">
            <div class="faq-question">如何修改订票信息?</div>
            <div class="faq-answer">您可在<span class="faq-link">个人页面</span>中进行修改，具体请查看相关操作指引。</div>
            <hr class="faq-divider" />
          </div>
          <div class="faq-entry" id="faq2">
            <div class="faq-question">订票后能否退票?</div>
            <div class="faq-answer">如需退票，请在订单详情页点击"申请退票"，具体请查看<span class="faq-link">退票规则</span>。</div>
            <hr class="faq-divider" />
          </div>
          <div class="faq-entry" id="faq3">
            <div class="faq-question">影片场次信息在哪里查看?</div>
            <div class="faq-answer">您可在首页或影片浏览页选择影片，点击进入详情页后查看所有场次信息。</div>
          </div>
        </div>
      </section>
    </el-card>

    <!-- Footer -->
    <footer class="starlight-footer">
      <div class="footer-content">
        © 2024 StarlightCinema ASAKI CINEMA | 专注优质电影票预订服务
      </div>
    </footer>

    <div
      v-if="ownedTooltip.show"
      class="owned-tooltip"
      :style="{ left: ownedTooltip.x + 'px', top: ownedTooltip.y + 'px' }"
    >已获得会员权益</div>

    <el-dialog v-model="showPayWaiting" title="等待支付完成" width="350px" :close-on-click-modal="false" :show-close="false">
      <div style="text-align:center;padding:24px 0;">
        <el-icon style="font-size:32px;color:#1e88e5;"><i class="el-icon-loading"></i></el-icon>
        <div style="margin-top:16px;">请在新窗口完成支付，支付成功后会员权益将自动生效。</div>
        <div style="margin-top:8px;color:#888;font-size:13px;">如已支付可关闭此窗口。</div>
        <el-button type="primary" style="margin-top:18px;" @click="manualCheckOrder">我已完成支付，手动确认</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import defaultMoviePng from '../assets/defaultMoviePng.webp'
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import { purchaseMembership } from '../api/membership'
import service from '../api/request'
import { ElMessage } from 'element-plus'
const activeIndex = ref('1');
const ctaButtonText = ref('登录网站');
const recommendedMovies = ref([
  { id: 1, title: '沙丘', poster_url: 'https://images.weserv.nl/?url=img3.doubanio.com/view/photo/s_ratio_poster/public/p2920455862.webp', rating: 4.5 },
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
const ownedTooltip = ref({ show: false, x: 0, y: 0 })
const ownedTooltipIdx = ref(null)

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
    buttonText: '立即登录'
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

function handlePricingMouseEnter(idx, e) {
  hoverIndex.value = idx
}
function handlePricingMouseLeave() {
  hoverIndex.value = null;
}

function handleOwnedCardMouseMove(idx, e) {
  if (isLoggedIn.value && memberLevel.value === idx) {
    ownedTooltip.value = {
      show: true,
      x: e.clientX + 12,
      y: e.clientY + 12
    }
    ownedTooltipIdx.value = idx
  }
}
function handleOwnedCardMouseLeave(idx, e) {
  if (ownedTooltipIdx.value === idx) {
    ownedTooltip.value.show = false
    ownedTooltipIdx.value = null
  }
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
const isLoggedIn = ref(false)
const memberLevel = ref(0)
const buyLoading = ref(false)
const showPayWaiting = ref(false)
const payOrderNo = ref('')

function checkLoginStatus() {
  isLoggedIn.value = !!localStorage.getItem('token')
  if (isLoggedIn.value) {
    // 假设登录后 member_level 存在 localStorage
    memberLevel.value = Number(localStorage.getItem('member_level') || 0)
  } else {
    memberLevel.value = 0
  }
}

function checkReturnUrlPage() {
  if (window.location.pathname === '/return_url') {
    // 检查是否在支付回跳页面
    setTimeout(() => {
      window.close();
      // 如果window.close无效（如非弹窗），可跳转首页
      window.location.href = '/';
    }, 2000);
  }
}

onMounted(() => {
  checkLoginStatus()
  checkReturnUrlPage()
})

function handleSwitchDialog(type) {
  if (type === 'login') {
    showRegister.value = false
    showLogin.value = true
  } else {
    showLogin.value = false
    showRegister.value = true
  }
}

function handleLoginDialogClose() {
  showLogin.value = false
  checkLoginStatus()
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('member_level')
  isLoggedIn.value = false
  memberLevel.value = 0
  showLogin.value = false
}

const getPlanButtonText = (plan, idx) => {
  if (memberLevel.value === 1) {
    if (plan.name.includes('VIP') && !plan.name.includes('SVIP')) return '续费';
    if (plan.name.includes('SVIP')) return '升级';
  } else if (memberLevel.value === 2) {
    if (plan.name.includes('VIP') && !plan.name.includes('SVIP')) return '不可降级';
    if (plan.name.includes('SVIP')) return '续费';
  }
  return plan.buttonText;
};

const isPlanButtonDisabled = (plan, idx) => {
  if (memberLevel.value === 2 && plan.name.includes('VIP') && !plan.name.includes('SVIP')) return true;
  return false;
};

function handleBuyClick(plan) {
  const token = localStorage.getItem('token')
  if (!token) {
    showLogin.value = true
    return
  }
  const userId = localStorage.getItem('user_id')
  if (!userId) {
    ElMessage.error('用户信息异常，请重新登录')
    handleLogout()
    return
  }
  let membershipType = ''
  if (plan.name.includes('VIP') && !plan.name.includes('SVIP')) membershipType = 'VIP'
  else if (plan.name.includes('SVIP')) membershipType = 'SVIP'
  else membershipType = 'FREE'
  if (membershipType === 'FREE') {
    ElMessage.info('基础会员无需购买')
    return
  }
  // SVIP用户不能降级
  if (memberLevel.value === 2 && membershipType === 'VIP') {
    ElMessage.warning('SVIP用户不能降级为VIP')
    return
  }
  // VIP升级SVIP，先查差价
  if (memberLevel.value === 1 && membershipType === 'SVIP') {
    service.post('/api/memberships/upgrade-price', {
      targetType: 'SVIP',
      targetDuration: pricingDuration.value
    }).then(res => {
      ElMessage.info(`升级SVIP需补差价：¥${res.upgradePrice}`)
      // 这里可弹窗确认，确认后再发起支付
      doPurchaseMembership(userId, membershipType, pricingDuration.value, 'ALIPAY')
    })
    return
  }
  // 普通购买/续费
  doPurchaseMembership(userId, membershipType, pricingDuration.value, 'ALIPAY')
}

function doPurchaseMembership(userId, membershipType, duration, paymentMethod) {
  buyLoading.value = true
  purchaseMembership({
    userId: Number(userId),
    membershipType,
    duration,
    paymentMethod
  }).then(res => {
    if (res && res.paymentUrl) {
      payOrderNo.value = res.orderNo
      showPayWaiting.value = true
      const payWin = window.open('', '_blank')
      payWin.document.write(res.paymentUrl)
      payWin.document.close()
      pollOrderStatus(res.orderNo, userId)
    } else {
      ElMessage.error('下单失败，未获取到支付链接')
    }
  }).catch(e => {
    ElMessage.error('下单失败，请重试')
    console.error(e)
  }).finally(() => {
    buyLoading.value = false
  })
}

function pollOrderStatus(orderNo, userId) {
  let timer = setInterval(async () => {
    try {
      const res = await service.get(`/api/payments/query/${orderNo}`)
      if (res && (res.status === 'COMPLETED' || res.status === 'TRADE_SUCCESS')) {
        clearInterval(timer)
        // 获取最新用户信息
        const userRes = await service.get(`/api/users/me`)
        if (userRes && typeof userRes.member_level !== 'undefined') {
          localStorage.setItem('member_level', userRes.member_level)
          memberLevel.value = userRes.member_level
          // 强制刷新页面，确保所有会员信息实时更新
          window.location.reload()
        }
        showPayWaiting.value = false
        ElMessage.success('支付成功，会员权益已生效！')
      }
    } catch (e) {
      // ignore
    }
  }, 10000)
}

function manualCheckOrder() {
  if (!payOrderNo.value) return;
  const userId = localStorage.getItem('user_id');
  service.get(`/api/payments/query/${payOrderNo.value}`).then(res => {
    if (res && (res.status === 'COMPLETED' || res.status === 'TRADE_SUCCESS')) {
      // 获取最新用户信息
      service.get(`/api/users/me`).then(userRes => {
        if (userRes && typeof userRes.member_level !== 'undefined') {
          console.log('[Home] 获取最新用户信息:', userRes)
          localStorage.setItem('member_level', userRes.member_level)
          memberLevel.value = userRes.member_level
          window.location.reload()
        }
        showPayWaiting.value = false
        ElMessage.success('支付成功，会员权益已生效！')
      })
    } else {
      ElMessage.warning('暂未检测到支付完成，请稍后再试或等待自动检测。')
    }
  }).catch(() => {
    ElMessage.error('订单状态查询失败，请稍后重试')
  })
}

function goMovieBrowse() {
  window.location.href = '/movies';
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

.hero-section-bg {
  background: url('https://images.unsplash.com/photo-1464983953574-0892a716854b?auto=format&fit=crop&w=1200&q=80') center/cover no-repeat;
  position: relative;
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

.hero-fade-to-card {
  position: absolute;
  left: 0; right: 0; bottom: 0; height: 10px;
  background: linear-gradient(to bottom, rgba(255,255,255,0) 0%, #f5f6fa 100%);
  pointer-events: none;
  z-index: 2;
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

.section-card {
  background: #f5f6fa;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(30,34,60,0.10);
  margin: 32px auto 0 auto;
  padding: 0;
  overflow: visible;
  max-width: 1200px;
  width: 100%;
}
.card-content {
  background: #fff;
  border-radius: 20px;
  margin: 32px;
  padding: 32px 24px;
  box-shadow: 0 4px 24px rgba(30,136,229,0.08);
}
@media (max-width: 1300px) {
  .section-card {
    max-width: 98vw;
  }
}
@media (max-width: 900px) {
  .card-content {
    margin: 16px;
    padding: 20px 8px;
  }
  .section-card {
    margin: 16px auto 0 auto;
    border-radius: 12px;
    max-width: 100vw;
  }
}

.faq-card {
  background: #f5f6fa;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(30,34,60,0.10);
  margin: 32px auto 0 auto;
  padding: 0;
  overflow: visible;
  max-width: 1200px;
  width: 100%;
}
.faq-section.card-content {
  background: #fff;
  border-radius: 20px;
  margin: 32px;
  padding: 32px 24px;
  box-shadow: 0 4px 24px rgba(30,136,229,0.08);
}
.faq-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  line-height: 1.3;
  margin: 0 0 24px 0;
}
.faq-intro {
  font-size: 17px;
  font-weight: normal;
  text-align: center;
  margin: 0 auto 32px auto;
  max-width: 80%;
}
.faq-list {
  width: 100%;
}
.faq-entry {
  margin-bottom: 24px;
}
.faq-question {
  font-size: 18px;
  font-weight: 600;
  margin: 16px 0 8px 0;
  cursor: pointer;
}
.faq-answer {
  font-size: 16px;
  font-weight: normal;
  line-height: 1.5;
  margin: 0 0 16px 0;
}
.faq-divider {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 0 0 0 0;
}
.faq-link {
  color: #1e88e5;
  cursor: pointer;
  text-decoration: underline;
}
@media (max-width: 900px) {
  .faq-section.card-content {
    margin: 16px;
    padding: 20px 8px;
  }
  .faq-card {
    margin: 16px auto 0 auto;
    border-radius: 12px;
    max-width: 100vw;
  }
}

.pricing-card.owned {
  border: 2.5px solid #FFD700 !important;
  box-shadow: 0 0 16px 0 rgba(255, 215, 0, 0.12);
  position: relative;
}
.owned-tooltip {
  position: fixed;
  z-index: 9999;
  background: #fffbe6;
  color: #bfa100;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 6px 16px;
  font-size: 15px;
  box-shadow: 0 2px 8px rgba(255,215,0,0.10);
  pointer-events: none;
  white-space: nowrap;
  font-weight: bold;
}

.hero-welcome {
  font-size: 20px;
  color: #fff;
  font-weight: bold;
  margin-top: 24px;
  letter-spacing: 1px;
}
</style> 