<template>
  <div class="student-layout">
    <!-- 左侧导航 -->
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">E</div>
        <div>
          <h1>EduAgent</h1>
          <p>个性化学习平台</p>
        </div>
      </div>

      <nav class="nav-menu">
        <RouterLink to="/student/dashboard" class="nav-item">
          <span>⌂</span>
          <em>学习总览</em>
        </RouterLink>

        <RouterLink to="/student/profile/overview" class="nav-item">
          <span>👥</span>
          <em>学习画像</em>
        </RouterLink>

        <RouterLink to="/student/resources" class="nav-item">
          <span>▣</span>
          <em>资源中心</em>
        </RouterLink>

        <RouterLink to="/student/path" class="nav-item">
          <span>◇</span>
          <em>学习路径</em>
        </RouterLink>

        <RouterLink to="/student/tutor" class="nav-item">
          <span>🤖</span>
          <em>智能辅导</em>
        </RouterLink>

        <RouterLink to="/student/report" class="nav-item">
          <span>☑</span>
          <em>学习评估</em>
        </RouterLink>

        <RouterLink to="/student/profile" class="nav-item">
          <span>☻</span>
          <em>个人中心</em>
        </RouterLink>
      </nav>

      <button class="collapse-btn">收起菜单</button>
    </aside>

    <!-- 右侧主体 -->
    <section class="layout-main">
      <header class="topbar">
        <div class="search-box">
          <input type="text" placeholder="搜索课程、资源、知识点..." />
          <span>⌕</span>
        </div>

        <div class="top-actions">
          <button class="notice-btn">
            🔔
            <i>3</i>
          </button>

          <div class="user-info">
            <div class="avatar">张</div>
            <span>张同学</span>
            <strong>⌄</strong>
          </div>
        </div>
      </header>

      <main class="page-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.student-layout {
  min-height: 100vh;
  display: flex;
  background: #f5f8ff;
  color: #1f2a44;
}

.sidebar {
  width: 230px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  background: #ffffff;
  border-right: 1px solid #e8eef7;
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
  color: #ffffff;
  font-weight: 800;
  background: linear-gradient(135deg, #1769ff, #45a3ff);
}

.brand h1 {
  margin: 0;
  color: #1769ff;
  font-size: 20px;
  line-height: 1;
}

.brand p {
  margin: 6px 0 0;
  color: #8a96a8;
  font-size: 12px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #52637a;
  text-decoration: none;
  transition: all 0.2s ease;
}

.nav-item span {
  width: 22px;
  text-align: center;
  font-style: normal;
}

.nav-item em {
  font-style: normal;
  font-size: 14px;
}

.nav-item:hover {
  color: #1769ff;
  background: #f2f7ff;
}

.nav-item.router-link-active {
  color: #ffffff;
  background: linear-gradient(135deg, #1769ff, #4a8cff);
  box-shadow: 0 10px 22px rgba(23, 105, 255, 0.22);
}

.collapse-btn {
  margin-top: auto;
  height: 40px;
  border: none;
  border-radius: 12px;
  color: #75849a;
  background: #f5f8ff;
  cursor: pointer;
}

.layout-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 72px;
  flex-shrink: 0;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e8eef7;
}

.search-box {
  width: 480px;
  height: 42px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #dbe4f3;
  border-radius: 999px;
  background: #f7faff;
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #1f2a44;
}

.search-box span {
  color: #8a96a8;
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

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #1f2a44;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1769ff;
  font-weight: 700;
  background: #eaf2ff;
}

.user-info span {
  font-size: 14px;
}

.user-info strong {
  color: #8a96a8;
}

.page-content {
  flex: 1;
  overflow: auto;
}

@media (max-width: 900px) {
  .sidebar {
    width: 78px;
    padding: 18px 10px;
  }

  .brand div:last-child,
  .nav-item em,
  .collapse-btn {
    display: none;
  }

  .brand {
    justify-content: center;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .topbar {
    padding: 0 16px;
  }

  .search-box {
    width: 260px;
  }
}
</style>