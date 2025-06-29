<template>
  <div class="nav-wrapper">
    <el-menu mode="horizontal" :default-active="activeIndex" class="main-nav container" background-color="#ffffff" text-color="#212121" active-text-color="#FF4081">
      <el-menu-item index="0" class="logo">CinemaBooking</el-menu-item>
      <div class="flex-grow" />
      <el-menu-item index="1" @click="goHome">首页</el-menu-item>
      <el-menu-item index="2" @click="goBrowse">影片浏览</el-menu-item>
      <el-menu-item index="3" @click="goToday">今日上映</el-menu-item>
      <el-menu-item index="6" v-if="isLoggedIn" @click="goProfile">个人中心</el-menu-item>
      <el-menu-item index="4" v-if="!isLoggedIn" @click="$emit('show-login')">登录</el-menu-item>
      <el-menu-item index="4" v-else @click="$emit('logout')">退出</el-menu-item>
      <el-menu-item index="5" @click="$emit('show-register')">注册</el-menu-item>
      <!-- 预留后续页面入口 -->
      <slot name="extra"></slot>
    </el-menu>
  </div>
</template>
<script setup>
import { defineProps } from 'vue';
const props = defineProps({
  isLoggedIn: Boolean,
  activeIndex: {
    type: String,
    default: '1'
  }
});
function goHome() {
  window.location.href = '/';
}
function goBrowse() {
  window.location.href = '/movies';
}
function goToday() {
  window.location.href = '/today';
}
function goProfile() {
  window.location.href = '/user/profile';
}
</script>
<style scoped>
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
</style> 