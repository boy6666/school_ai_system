# ── 前端三端（Vue3 + Vite + Element Plus）多阶段构建 ──
# 构建阶段
FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# 若 vue-tsc 类型检查报错阻断构建，可临时改为：RUN npx vite build
RUN npm run build

# 运行阶段：nginx 托管静态资源并反向代理后端
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
