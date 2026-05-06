<template>
  <div class="admin-layout">
    <!-- 左侧菜单 -->
    <aside class="admin-sidebar">
      <div class="brand">
        <div class="brand-icon">A</div>
        <div class="brand-text">
          <strong>EduAgent</strong>
          <span>管理后台</span>
        </div>
      </div>

      <nav class="admin-menu">
        <RouterLink to="/admin/dashboard" class="menu-item">
          <span>⌂</span>
          <em>后台首页</em>
        </RouterLink>

        <RouterLink to="/admin/users" class="menu-item">
          <span>👥</span>
          <em>用户管理</em>
        </RouterLink>

        <RouterLink to="/admin/resources" class="menu-item">
          <span>▣</span>
          <em>课程 / 资源管理</em>
        </RouterLink>

        <RouterLink to="/admin/agents" class="menu-item">
          <span>🤖</span>
          <em>智能体管理</em>
        </RouterLink>

        <RouterLink to="/admin/reviews" class="menu-item">
          <span>☑</span>
          <em>内容审核</em>
        </RouterLink>

        <RouterLink to="/admin/statistics" class="menu-item">
          <span>📊</span>
          <em>数据统计</em>
        </RouterLink>

        <RouterLink to="/admin/settings" class="menu-item">
          <span>⚙</span>
          <em>系统设置</em>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <span>当前角色</span>
        <strong>系统管理员</strong>
      </div>
    </aside>

    <!-- 右侧主体 -->
    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>EduAgent 智能学习平台管理中心</p>
        </div>

        <div class="top-actions">
          <button class="notice-btn">
            🔔
            <i>5</i>
          </button>

          <div class="admin-user">
            <div class="avatar">管</div>
            <div>
              <strong>管理员</strong>
              <span>admin@edu-agent.com</span>
            </div>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => {
  return String(route.meta.title || '管理后台')
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  background: #f5f8ff;
  color: #1f2a44;
}

.admin-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  background: #101828;
  color: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 34px;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1769ff;
  font-weight: 800;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-text strong {
  font-size: 18px;
}

.brand-text span {
  color: #aab4c5;
  font-size: 12px;
}

.admin-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #c9d2e3;
  text-decoration: none;
  transition: all 0.2s ease;
}

.menu-item span {
  width: 22px;
  text-align: center;
}

.menu-item em {
  font-size: 14px;
  font-style: normal;
}

.menu-item:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.menu-item.router-link-active {
  color: #ffffff;
  background: #1769ff;
  box-shadow: 0 10px 22px rgba(23, 105, 255, 0.28);
}

.sidebar-footer {
  margin-top: auto;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.sidebar-footer span {
  display: block;
  margin-bottom: 6px;
  color: #aab4c5;
  font-size: 12px;
}

.sidebar-footer strong {
  font-size: 14px;
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  min-height: 72px;
  padding: 14px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  background: #ffffff;
  border-bottom: 1px solid #e8eef7;
}

.admin-topbar h2 {
  margin: 0;
  font-size: 22px;
}

.admin-topbar p {
  margin: 5px 0 0;
  color: #75849a;
  font-size: 13px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.notice-btn {
  position: relative;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #f5f8ff;
  cursor: pointer;
}

.notice-btn i {
  position: absolute;
  top: -4px;
  right: -2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: #ffffff;
  background: #ff4d4f;
  font-size: 12px;
  font-style: normal;
  line-height: 18px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1769ff;
  background: #eaf2ff;
  font-weight: 700;
}

.admin-user strong {
  display: block;
  font-size: 14px;
}

.admin-user span {
  color: #8a96a8;
  font-size: 12px;
}

.admin-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

@media (max-width: 1100px) {
  .admin-sidebar {
    width: 84px;
    padding: 18px 10px;
  }

  .brand {
    justify-content: center;
  }

  .brand-text,
  .menu-item em,
  .sidebar-footer {
    display: none;
  }

  .menu-item {
    justify-content: center;
    padding: 0;
  }

  .menu-item span {
    width: auto;
  }
}

@media (max-width: 768px) {
  .admin-layout {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
    height: auto;
    padding: 12px;
  }

  .brand {
    justify-content: flex-start;
    margin-bottom: 12px;
  }

  .brand-text {
    display: flex;
  }

  .admin-menu {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .menu-item {
    min-width: 86px;
    height: 38px;
    flex-shrink: 0;
    justify-content: center;
    padding: 0 12px;
  }

  .menu-item span {
    display: none;
  }

  .menu-item em {
    display: inline;
    font-size: 13px;
  }

  .admin-topbar {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px;
  }

  .top-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .admin-user span {
    display: none;
  }

  .admin-topbar h2 {
    font-size: 20px;
  }
}
</style>