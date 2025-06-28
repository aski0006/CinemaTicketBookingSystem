<template>
  <div class="movie-detail-card" v-if="movie">
    <div class="left">
      <img :src="movie.posterUrl || movie.poster_url" :alt="movie.title" class="poster" />
    </div>
    <div class="right">
      <div class="title">{{ movie.title }}</div>
      <div class="subheading">
        <span>导演：</span>{{ movie.director }}
      </div>
      <div class="subheading">
        <span>主演：</span>{{ movie.actors }}
      </div>
      <div class="genres">
        <span>类型：</span>{{ movie.genre || movie.genres }}
      </div>
      <div class="meta">
        <span>时长：</span>{{ movie.duration }} 分钟
        <span class="release">上映日期：{{ movie.releaseDate || movie.release_date }}</span>
      </div>
      <div class="rating-block">
        <span class="rating-label">评分：</span>
        <div class="rating-bar">
          <div class="rating-bar-inner" :style="{ width: (movie.rating * 10) + '%' }"></div>
        </div>
        <span class="rating-value">{{ movie.rating ? movie.rating.toFixed(1) : 'N/A' }}</span>
      </div>
      <div class="desc-title">简介</div>
      <div class="desc">{{ movie.description }}</div>
      <el-button class="close-btn" @click="$emit('close')">{{ closeButtonText }}</el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  movie: Object,
  closeButtonText: {
    type: String,
    default: '关闭'
  }
});
defineEmits(['close']);
</script>

<style scoped>
.movie-detail-card {
  display: flex;
  background: #fff;
  color: #000;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.12);
  padding: 32px;
  max-width: 800px;
  margin: 40px auto;
  gap: 32px;
}
.left {
  flex: 2;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.poster {
  width: 240px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.right {
  flex: 3;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.title {
  font-size: 2rem;
  font-weight: bold;
  color: #000;
  margin-bottom: 8px;
}
.subheading {
  font-size: 1.1rem;
  font-weight: bold;
  color: #000;
  margin-bottom: 2px;
}
.genres {
  font-size: 1rem;
  color: #000;
  margin-bottom: 2px;
}
.meta {
  font-size: 1rem;
  color: #808080;
  margin-bottom: 8px;
}
.release {
  margin-left: 16px;
}
.rating-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.rating-label {
  color: #000;
  font-weight: bold;
}
.rating-bar {
  width: 120px;
  height: 12px;
  background: #eee;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 8px;
}
.rating-bar-inner {
  height: 100%;
  background: #FFD700;
  width: 0;
  transition: width 0.3s;
}
.rating-value {
  color: #FF0000;
  font-weight: bold;
}
.desc-title {
  font-size: 1.1rem;
  font-weight: bold;
  margin-top: 8px;
  color: #000;
}
.desc {
  font-size: 1rem;
  color: #000;
  line-height: 1.6;
  margin-bottom: 12px;
}
.close-btn {
  margin-top: 16px;
  background: #FF4081;
  color: #fff;
  border: none;
}
.close-btn:hover {
  background: #f50057;
}
</style> 