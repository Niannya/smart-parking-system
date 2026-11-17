<script setup>
// 临时入口：前端路由由成员 1 统一规划，本阶段先把「停车区域」页面挂在这里，便于单模块联调。
// 路由骨架合并后，本文件改回 <router-view />，本页面移动到 /parking/areas 路由下。
// 原先请求 /api/instructors 的教师列表是框架连通测试代码，smart_parking 库中没有 admin 表，会报 500，已移除。
import AreaManage from './views/parking/AreaManage.vue'
</script>

<template>
  <AreaManage />
</template>
