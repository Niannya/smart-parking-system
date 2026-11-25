<script setup>
// 过渡桥接：App.vue 由成员 1 维护，在路由骨架合并前仍挂载本文件。
// 真正的页面在 src/views/admin/ParkingAreasView.vue；成员 1 接入 Vue Router 后，
// 路由 /admin/areas 直接指向 ParkingAreasView.vue，本文件随之删除。
import ParkingAreasView from '../admin/ParkingAreasView.vue'
</script>

<template>
  <ParkingAreasView />
</template>
