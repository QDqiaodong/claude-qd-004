<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">奶牛场 · 牛舍挤奶位与饲料管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/barns">牛舍与挤奶位</el-menu-item>
        <el-menu-item index="/cows">奶牛档案</el-menu-item>
        <el-menu-item index="/shifts">挤奶班次</el-menu-item>
        <el-menu-item index="/feeds">饲料领用</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
