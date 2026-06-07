<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
/* Global overrides — App.vue loads last, overrides Element Plus and style.css */
html, body, #app {
  width: 100%;
  min-height: 100vh;
}

/* Notion-style scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--hairline-strong); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--stone); }

/* Selection color */
::selection { background: var(--tint-lavender); color: var(--primary-deep); }

/* Links in cards */
.el-card a { color: var(--link-blue); }
.el-card a:hover { color: var(--link-blue-pressed); }
</style>
