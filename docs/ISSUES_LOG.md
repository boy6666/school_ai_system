# EduAgent 问题排查日志（Troubleshooting Log）

> 目的：把排查中踩过的坑、根因、处理方式、涉及版本**完整归档**，供后续开发者直接复用，
> 避免重复排查。每一条按「现象 → 根因 → 处理 → 结论」记录。
>
> 环境基准：本机单环境 local（方案 B：中间件 Docker + Java 服务 IDE 直跑 + Nacos 命名空间 `edu-agent-local`）。
> 涉及版本：Spring Boot 3.2.5 / Spring Cloud 2023.0.3 / Spring Cloud Alibaba 2023.0.3.2 / MyBatis-Plus 3.5.9。

---

## 问题 1：`factoryBeanObjectType` 异常导致所有 Java 服务起不来（★ 最严重）

**现象**
Java 服务启动时，Spring 上下文在刷新阶段崩，进程直接退出：

```
java.lang.IllegalArgumentException: Invalid value type for attribute 'factoryBeanObjectType': java.lang.String
	at FactoryBeanRegistrySupport.getTypeForFactoryBeanFromAttributes
	at AbstractAutowireCapableBeanFactory.getTypeForFactoryBean
	at AbstractBeanFactory.isTypeMatch
	at PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors
```

发生在任何业务代码之前，所有 DB 服务（auth/learning/resource/teacher/code）都会撞，与业务无关。

**根因（完整因果链）**
1. Spring Cloud OpenFeign 的 `FeignClientsRegistrar`、以及 **MyBatis 的 mapper 扫描器（`ClassPathMapperScanner`）**在注册 `FactoryBean` 类型的 Bean 定义时，会把 `factoryBeanObjectType` 属性写成**字符串类名**（String）。
2. Spring Framework 6.1 / Spring Boot 3.2 起，`getTypeForFactoryBeanFromAttributes` 对该属性做**强类型校验**（必须是 `Class`/`ResolvableType`），遇到 String 直接抛 `IllegalArgumentException`。Boot 3.1 及以前对 String 静默宽限，所以旧版本不炸。
3. 本工程当时解析到的 **`org.mybatis:mybatis-spring:2.1.2`** 是给 Spring 5 用的老版本，不含该适配；它由 `mybatis-plus-boot-starter`（Boot 2 时代 starter）拉入。

**排查过程（避免再走弯路）**
- ❌ 误判①：怀疑 openfeign → 它是已知元凶（spring-cloud-openfeign #912/#950），但**修复版 4.1.1 已在 classpath**，且升级后错误仍在，可排除。
- ❌ 误判②：怀疑 Spring Cloud Alibaba nacos-config → 升级 SCA 后错误仍在，可排除。
- ✅ 真凶是 **mybatis-spring 2.1.2 + Boot 2 版 mybatis-plus starter**：`mybatis-plus-boot-starter:3.5.7` 拉的 mybatis-spring 2.1.2（2022 年，面向 Spring 5）。

**处理（修复 + 一并升级）**
1. **MyBatis-Plus 换 Boot 3 专用 starter + 升版本**（这是真正的修复）：
   - `mybatis-plus-boot-starter` → **`mybatis-plus-spring-boot3-starter`**（common-mybatis 与 auth/learning/resource/teacher/code 5 个服务 pom + 父 pom dependencyManagement 全部替换）
   - `mybatis-plus.version` 3.5.7 → **3.5.9**（配套 mybatis-spring **3.0.4**）
2. **补分页构件**：3.5.9 起 `PaginationInnerInterceptor`（依赖 JSqlParser）被抽到独立构件 `mybatis-plus-jsqlparser`，`MybatisPlusConfig` 用的 `com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor` 会编译不过 → 新增依赖 `mybatis-plus-jsqlparser`（版本 3.5.9，父 pom dependencyManagement 管理）。
3. **顺带升级框架族**（非本次修复必要，属版本卫生）：`spring-cloud.version` 2023.0.1 → **2023.0.3**，`spring-cloud-alibaba.version` 2023.0.1.0 → **2023.0.3.2**。效果 openfeign-core 4.1.1→4.1.3、nacos-client 2.3.2→2.4.2、loadbalancer 4.1.2→4.1.4。
   - ⚠️ 注意：SCA 没有 `2023.0.3.0`，需用 `2023.0.3.x`（2023.0.3.2 已验证在镜像上）。

**结论**
- 升级后 auth 正常启动：`Started AuthApplication in 5.715 seconds`，Tomcat 8081，Flyway 对 `auth_db` 建表成功，注册进 Nacos，`/actuator/health` = `UP`。
- **教训：Boot 3 工程务必用 `mybatis-plus-spring-boot3-starter`，不要照抄教程里的 `mybatis-plus-boot-starter`；碰到 `factoryBeanObjectType` 报错，优先查 mybatis-spring 版本（需 ≥3.0.3）。**

**涉及版本对照**
| 依赖 | 修复前 | 修复后 |
|---|---|---|
| mybatis-plus | 3.5.7（boot-starter） | 3.5.9（spring-boot3-starter） |
| mybatis-spring | 2.1.2 | 3.0.4 |
| spring-cloud | 2023.0.1 | 2023.0.3 |
| spring-cloud-alibaba | 2023.0.1.0 | 2023.0.3.2 |
| openfeign-core | 4.1.1 | 4.1.3 |
| nacos-client | 2.3.2 | 2.4.2 |

---

## 问题 2：Nacos 2.x 的 gRPC 端口（9848）未暴露 → 服务连不上配置中心

**现象**
Java 服务启动报：`Connection refused: getsockopt: /127.0.0.1:9848`，随后 `Fail to connect server ... port 8848`。Nacos 控制台 HTTP 8848 明明是 200。

**根因**
Nacos 2.x 客户端不止走 HTTP 8848，还走 **gRPC 9848**（=8848+1000）。`docker-compose.yml` 只映射 `8848:8848`，宿主 9848 没转发进容器 → 服务经 gRPC 拿不到配置/注册失败。

**处理**
`deploy/docker-compose.yml` 的 nacos 补：
```yaml
ports:
  - "8848:8848"   # HTTP 控制台/API
  - "9848:9848"   # gRPC 客户端端口（Nacos 2.x 必需）
  - "9849:9849"   # gRPC 服务端端口（可选）
```
重建容器后 gRPC 9848 可达。

**结论**：单暴露 8848 是不够的，容器化 Nacos 必须连 9848 一起暴露，否则 Java 服务全部连不上。

---

## 问题 3：Nacos 配置为「空」→ 服务拉不到 datasource / JWT 密钥

**现象**
日志：`[Nacos Config] config[dataId=edu-agent-auth.yaml, group=edu-agent] is empty`；服务启动拿不到任何配置。

**根因**
配置没真正落到应用查询的 `(namespace=edu-agent-local, group=edu-agent, dataId=*.yaml)` 组合里（推送环节不完整/未落库）。

**处理**
1. 重跑 `python deploy/push-nacos-config.py`，确认 6 个 data-id 全部 `[OK]`，再用 HTTP API `GET /nacos/v1/cs/configs?dataId=...&group=...&tenant=...` 逐个取回验证非空。
2. 修正 datasource 默认账号：5 个 `deploy/nacos-config/*.yaml` 里 `${DB_USER:root}/${DB_PASSWORD:root}` → `${DB_USER:edu_agent}/${DB_PASSWORD:edu_agent}`（本机业务账号是 `edu_agent/edu_agent`，root 密码不是 root），改后重推。这样开发者 IDE 直跑无需设环境变量。

**结论**：推送后用 API 取回验证，别信脚本单行 `[OK]`；yaml 里本地默认账号统一为 `edu_agent/edu_agent`。

---

## 问题 4：网关验签 401「未认证或令牌无效」—— 网关与 auth 的 JWT 密钥不一致

**现象**
register / login 均正常（直连 auth 8081 拿到 token、用户落库），但携带**同一份**有效 JWT 经网关 8080 调受保护端点时，被网关拦截返回 `{"code":401,"message":"未认证或令牌无效","data":null}`；无 token 也 401。

**根因**
JWT 验签密钥不一致：
- **auth 签发端**：Nacos `edu-agent-auth.yaml` 配了 `edu-agent.jwt.secret = ${JWT_SECRET:edu-agent-local-secret-please-change}`，用 `edu-agent-local-secret-please-change` 签名。
- **网关验签端**：Nacos `edu-agent-gateway.yaml` 原**只有 auth whitelist，没有 `edu-agent.jwt.secret`** → `JwtUtil` 回退到占位默认值 `change-me-this-is-a-placeholder-secret-please-set-in-nacos`。

签 / 验用不同密钥 → HS256 签名校验失败 → 网关判其为未认证。

**处理**
1. `deploy/nacos-config/edu-agent-gateway.yaml` 补上与 auth 完全一致的密钥：
   ```yaml
   edu-agent:
     jwt:
       secret: ${JWT_SECRET:edu-agent-local-secret-please-change}
   ```
2. 重推配置：`python deploy/push-nacos-config.py`（并 HTTP API 取回验证）。
3. **重启网关**：`JwtUtil.secret` 是普通 `@Value`（非 `@RefreshScope`），改配置不热生效，必须重启 gateway。

**结论**
所有涉及 JWT 的服务（至少 auth 签发 + gateway 验签，联调时各机器）必须共享同一份 `edu-agent.jwt.secret`。排查 401 先对比签 / 验两端的 secret 是否一致。

---

## 问题 5：网关“中文 body 被改坏” —— 真凶是测试端（Windows git-bash 发 GBK），不是服务端

**现象**
经网关 8080 用**内联中文** POST `/api/edu-agent-auth/register` 报 HTTP-500 / 中文 `realName` 存成乱码；但**同样中文直连 auth 8081 却正常**。一度怀疑「网关把 UTF-8 重编码成 GBK」。

**根因（完整因果链）**
1. Java 侧无法在 JVM 默认 UTF-8 下凭空把 UTF-8 变 GBK——只有显式 `Charset.forName("GBK")` / `getBytes("GBK")` 才产出 GBK 字节。
2. 用 **Arthas 挂网关 JVM**（`watch java.nio.charset.CharsetEncoder encode`、`watch Charset.forName`，需 `options unsafe true` + `-x 1`，`-x 3` 会因 `module java.base does not "opens java.nio"` 崩）全程监听：**网关进程 0 次调用 GBK encoder** → 服务端根本没有重编码。
3. 真凶在**客户端**：Windows git-bash 下执行 `curl.exe --data '...中文...'`，内联 argv 经系统 ANSI 代码页（中文 Windows = GBK）转码后写 socket。bash 变量是 UTF-8 不代表 curl 写上网的是 UTF-8。用 `--data-binary @utf8file`（原样读取文件字节）走**同一个**网关 + auth 立即 `code:0`、`realName` 正确入库。

**排查过程（关键教训）**
- ❌ 先赖服务端（网关/编码器）→ 走了大弯路。**先验证客户端真正写上 socket 的字节**，再怀疑中间件。
- `--trace-ascii` 会把不可打印字节变成点号 → 无效；用 `--data-binary @file` 作为决定性“阳性对照”。
- `Content-Type: application/json` 带不带 `; charset=utf-8`：两者都返回 200（`GlobalExceptionHandler` 一律包 200），必须**读响应体**（`code:500`）判断。

**处理**
- 排查用临时诊断 Filter（`RequestBodyDumpFilter`，嗅探原始字节后重放）已删除；容器内 `/opt/jdk17`（为 Arthas attach 临时装的 JDK）与 arthas jar 已清理；auth 容器重新构建为干净镜像。

**结论**
- **测试端发内联中文时，先确认客户端落盘/上网的字节是 UTF-8 还是系统代码页，别先怪服务端。**Shell 变量是 UTF-8 ≠ 发出的是 UTF-8。抓服务端收到的原始 hex 才是铁证。
- 中文负载一律用 `--data-binary @file`（或程序内显式 UTF-8）发送。

---

## 问题 6：网关 `/me` 全部令牌都 401 —— 缺 `jjwt-impl.jar`，验签根本没执行

**现象**
经网关 8080：`register` / `login` 均正常（`code:0`、中文正确入库、拿到 token），但携带**任意**有效 JWT 调 `/me` 一律返回 `{"code":401,"message":"未认证或令牌无效"}`。**默认密钥、deploy 密钥、auth 实际签发 token 三把全部被拒**，非常迷惑。

**根因（完整因果链）**
1. 先排除密钥：用 Python HMAC 验证真实 login token 与 `edu-agent-deploy-secret-please-change` 完全匹配（auth 签得对）；用 Arthas `vmtool` 读网关 JVM 运行期 `JwtUtil.secret` 字段，也**正是**该 deploy 密钥。→ **密钥两边一致，不是密钥问题。**
2. 用 Arthas `watch JwtUtil.parseToken -e 'throwExp'` 抓到真异常：
   ```
   io.jsonwebtoken.lang.UnknownClassException: Unable to load class named
   [io.jsonwebtoken.impl.DefaultJwtParserBuilder] ... Have you remembered to
   include the jjwt-impl.jar in your runtime classpath?
       at Jwts.parser(Jwts.java:1069) → JwtUtil.parseToken(JwtUtil.java:44)
   ```
3. 根因是**依赖缺漏**：`edu-agent-common/pom.xml` 只声明 `jjwt-api`（编译期）；`edu-agent-auth` 额外带了 `jjwt-impl`+`jjwt-jackson`（runtime，能签能验）；**`edu-agent-gateway/pom.xml` 一个都没带** → 网关只有 jjwt-api，`Jwts.parser()` 反射加载 `io.jsonwebtoken.impl.DefaultJwtParserBuilder` 失败，**在验签之前就抛异常**，与令牌、密钥、secret 全部无关。

**排查过程（教训）**
- ❌ 误判①：密钥不一致 → 铁证排除（HMAC 验签 + Arthas 读字段，两边都是 deploy）。
- ❌ 误判②：旧构建 / 残留 arthas instrumentation / `JWT_SECRET` 空值 / `WeakKeyException` → 干净重建网关后依旧 401，排除。
- ✅ 真凶是**类加载/依赖缺失**，不是业务逻辑。**抓异常的唯一可靠法是看被捕获异常的堆栈**（过滤器静默吞异常，日志无痕，必须 Arthas `watch -e 'throwExp'` 或临时打日志）。
- 附注：网关日志里反复出现 `java.lang.instrument ASSERTION FAILED` 是**无关噪音**，不是本问题成因。

**处理**
- `edu-agent-gateway/pom.xml` 补上与 auth 一致的运行时依赖（版本由父 pom `<jjwt.version>0.12.5</jjwt.version>` 管理）：
  ```xml
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><scope>runtime</scope></dependency>
  ```
- 重建网关镜像并重建容器，复测通过：`/me` → `code:0` 带出 `realName 欧阳锋`、`roles ROLE_STUDENT`。

**结论**
- **凡是在运行时使用 `JwtUtil`（`Jwts.builder()` 签发或 `Jwts.parser()` 验签）的服务，classpath 必须同时包含 `jjwt-impl` 与 `jjwt-jackson`**；`edu-agent-common` 只把 `jjwt-api` 作为编译期依赖，实现 jar 需要各消费服务自行按 runtime 补。
- 排查「网关/某服务验签全 401」时，若 key 已确认一致仍失败，优先怀疑**缺 jjwt 运行时实现 jar**，直接看 `Jwts.parser()` 的类加载异常，别再纠结密钥。

---

## 附：一次性环境注意点（非缺陷，但卡人）

- **本机 MySQL**（方案 B 复用它，不做 Docker）：`database/init-microservice.sql` 会建 5 库（`auth_db/learning_db/resource_db/teacher_db/code_db`）+ 账号 `edu_agent/edu_agent`，需用本机 MySQL 的 root 执行一次。
- root 连接验证：`mysql -uroot -p1234`（本机 root 口令 1234，见 `.env` 对应项，生产勿复用）。
- Docker Desktop 引擎曾中途掉线（named pipe 消失、容器全停）→ 属环境抖动，重启 Docker Desktop 后 `docker compose up -d` 拉回即可，与代码无关。
