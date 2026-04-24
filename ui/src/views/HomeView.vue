<script setup lang="ts">
import { axiosInstance } from '@halo-dev/api-client'
import { computed, onMounted, ref } from 'vue'

const eventItems = [
  '用户登录：基于 Halo 官方共享事件 UserLoginEvent',
  '新设备登录：监听 Device 资源新增',
  '修改密码：监听 User 资源更新并比较密码字段变化',
  '收到评论：监听 Comment 资源新增',
  '评论被回复：监听 Reply 资源新增',
]

const formatItems = [
  {
    title: '通用 JSON',
    description: '适合自建 Webhook 服务、自动化平台或二次转发服务。',
    example: '{ "event": "comment_created", "title": "...", "markdown": "..." }',
  },
  {
    title: 'ntfy Markdown',
    description: '适合 ntfy，正文直接发送 Markdown，并自动附加 Title / Tags / Click 头。',
    example: 'Title: 收到评论\\nMarkdown: yes\\nBody: ## 收到评论 ...',
  },
  {
    title: 'Slack 兼容',
    description: '适合 Slack Incoming Webhook 或兼容 text 字段的平台。',
    example: '{ "text": "## 收到评论\\n- author：Alice" }',
  },
]

const headerExample = `{
  "Authorization": "Bearer your-token",
  "X-Source": "halo"
}`

type SavedEndpoint = {
  index: number
  enabled: boolean
  name?: string
  url?: string
  format?: string
  headersJson?: string
}

type TestResult = {
  success: boolean
  statusCode?: number
  message?: string
  responseBody?: string
  errorType?: string
}

const endpoints = ref<SavedEndpoint[]>([])
const loadingEndpoints = ref(false)
const testingIndex = ref<number | null>(null)
const testResults = ref<Record<number, TestResult>>({})

function resultClass(result?: TestResult) {
  return result?.success ? 'result result--success' : 'result result--error'
}

async function loadEndpoints() {
  loadingEndpoints.value = true

  try {
    const { data } = await axiosInstance.get('/apis/api.console.halo.run/v1alpha1/plugins/webhook-notification/webhook-endpoints')
    endpoints.value = data
  } catch (error: any) {
    endpoints.value = []
  } finally {
    loadingEndpoints.value = false
  }
}

async function handleTestWebhook(index: number) {
  testingIndex.value = index
  delete testResults.value[index]

  try {
    const { data } = await axiosInstance.post(
      `/apis/api.console.halo.run/v1alpha1/plugins/webhook-notification/test-webhook/${index}`,
    )
    testResults.value[index] = data
  } catch (error: any) {
    testResults.value[index] = error?.response?.data ?? {
      success: false,
      message: error?.message ?? '请求失败',
      errorType: error?.name ?? 'RequestError',
    }
  } finally {
    testingIndex.value = null
  }
}

const enabledCount = computed(() => endpoints.value.filter((item) => item.enabled).length)

onMounted(() => {
  loadEndpoints()
})
</script>

<template>
  <section class="guide">
    <div class="hero">
      <p class="eyebrow">Webhook Notification</p>
      <h1>Halo 事件通知已接入插件设置页</h1>
      <p class="lead">
        正式配置入口在当前插件的「设置」表单。这里提供支持的事件、Webhook 格式和 Header
        示例，方便你快速对接 ntfy、Slack 或自定义服务。
      </p>
    </div>

    <div class="grid">
      <article class="card">
        <h2>支持事件</h2>
        <ul>
          <li v-for="item in eventItems" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="card">
        <h2>配置方式</h2>
        <ul>
          <li>可配置多个 Webhook 目标，每个目标独立选择通知格式。</li>
          <li>每个目标支持单独配置可选请求头，格式为 JSON 对象。</li>
          <li>事件开关在插件设置里统一控制，关闭后不会再向任何 Webhook 发送该类通知。</li>
        </ul>
      </article>
    </div>

    <article class="formats">
      <div class="formats__header">
        <p class="eyebrow">Formats</p>
        <h2>内置通知格式</h2>
      </div>
      <div class="formats__grid">
        <section v-for="format in formatItems" :key="format.title" class="format-card">
          <h3>{{ format.title }}</h3>
          <p>{{ format.description }}</p>
          <pre>{{ format.example }}</pre>
        </section>
      </div>
    </article>

    <article class="tester">
      <div class="tester__intro">
        <p class="eyebrow">Tester</p>
        <h2>测试当前已保存的 Webhook 目标</h2>
        <p>这里读取的是插件设置页里已经保存的 Webhook 配置。修改设置后请先保存，再回到这里点击测试。</p>
      </div>

      <div class="tester__toolbar">
        <span>已保存 {{ endpoints.length }} 个目标，其中启用 {{ enabledCount }} 个</span>
        <button class="ghost-button" type="button" :disabled="loadingEndpoints" @click="loadEndpoints">
          {{ loadingEndpoints ? '刷新中...' : '刷新目标列表' }}
        </button>
      </div>

      <div v-if="!endpoints.length" class="empty-state">
        <strong>当前还没有已保存的 Webhook 目标。</strong>
        <p>请先到插件设置页新增并保存 Webhook 配置，然后再回来测试。</p>
      </div>

      <div v-else class="target-list">
        <section v-for="endpoint in endpoints" :key="endpoint.index" class="target-card">
          <div class="target-card__header">
            <div>
              <h3>{{ endpoint.name || `Webhook #${endpoint.index + 1}` }}</h3>
              <p class="target-card__meta">
                <span>{{ endpoint.enabled ? '已启用' : '未启用' }}</span>
                <span>{{ endpoint.format || 'generic-json' }}</span>
              </p>
            </div>
            <button
              class="primary-button"
              type="button"
              :disabled="testingIndex === endpoint.index"
              @click="handleTestWebhook(endpoint.index)"
            >
              {{ testingIndex === endpoint.index ? '测试中...' : '测试当前配置' }}
            </button>
          </div>

          <p class="target-card__url">{{ endpoint.url || '未配置地址' }}</p>

          <details v-if="endpoint.headersJson" class="headers-preview">
            <summary>查看请求头 JSON</summary>
            <pre>{{ endpoint.headersJson }}</pre>
          </details>

          <div v-if="testResults[endpoint.index]" :class="resultClass(testResults[endpoint.index])">
            <div class="result__headline">
              <strong>{{ testResults[endpoint.index].success ? '连接成功' : '连接失败' }}</strong>
              <span v-if="typeof testResults[endpoint.index].statusCode === 'number'">
                HTTP {{ testResults[endpoint.index].statusCode }}
              </span>
            </div>
            <p v-if="testResults[endpoint.index].message" class="result__message">
              {{ testResults[endpoint.index].message }}
            </p>
            <p v-if="testResults[endpoint.index].errorType" class="result__meta">
              错误类型：{{ testResults[endpoint.index].errorType }}
            </p>
            <pre v-if="testResults[endpoint.index].responseBody">
{{ testResults[endpoint.index].responseBody }}
            </pre>
          </div>
        </section>
      </div>
    </article>

    <article class="headers">
      <div>
        <p class="eyebrow">Headers</p>
        <h2>自定义请求头示例</h2>
        <p>如需鉴权或标记来源，可在每个 Webhook 目标上填写 JSON 形式的请求头。</p>
      </div>
      <pre>{{ headerExample }}</pre>
    </article>
  </section>
</template>

<style lang="scss" scoped>
.guide {
  min-height: 100vh;
  padding: 32px;
  color: #14213d;
  background:
    radial-gradient(circle at top left, rgb(249 200 70 / 0.24), transparent 32%),
    linear-gradient(180deg, #fffdf5 0%, #f6f8fc 100%);
}

.hero {
  max-width: 860px;
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #c26d00;
}

h1,
h2,
h3 {
  margin: 0;
  font-family: Georgia, 'Times New Roman', serif;
}

h1 {
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.05;
}

.lead {
  max-width: 720px;
  margin-top: 14px;
  font-size: 16px;
  line-height: 1.7;
  color: #46546c;
}

.grid,
.formats__grid {
  display: grid;
  gap: 18px;
}

.grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-bottom: 24px;
}

.card,
.format-card,
.headers,
.tester {
  border: 1px solid rgb(20 33 61 / 0.08);
  border-radius: 24px;
  background: rgb(255 255 255 / 0.9);
  box-shadow: 0 18px 40px rgb(20 33 61 / 0.06);
  backdrop-filter: blur(12px);
}

.card,
.format-card {
  padding: 22px;
}

.tester,
.headers {
  padding: 22px;
}

.card ul,
.headers p {
  color: #46546c;
}

ul {
  padding-left: 18px;
  margin: 16px 0 0;
  line-height: 1.8;
}

.formats {
  margin-bottom: 24px;
}

.formats__header {
  margin-bottom: 14px;
}

.formats__grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.format-card p {
  min-height: 72px;
  margin: 12px 0;
  line-height: 1.7;
  color: #46546c;
}

.tester {
  margin-bottom: 24px;
}

.tester__intro p:last-child {
  margin: 12px 0 0;
  color: #46546c;
  line-height: 1.7;
}

.tester__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  color: #46546c;
}

.ghost-button,
.primary-button {
  border: 0;
  border-radius: 999px;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid rgb(20 33 61 / 0.12);
  background: #fff;
  color: #14213d;
}

.ghost-button:disabled,
.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.empty-state {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: #fff8e8;
  border: 1px solid #ecd9a4;
}

.empty-state p {
  margin: 8px 0 0;
  color: #46546c;
}

.target-list {
  margin-top: 18px;
  display: grid;
  gap: 16px;
}

.target-card {
  padding: 18px;
  border-radius: 20px;
  background: #f9fbff;
  border: 1px solid rgb(20 33 61 / 0.08);
}

.target-card__header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
}

.target-card__header h3 {
  font-size: 20px;
}

.target-card__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 8px 0 0;
  color: #67758c;
  font-size: 13px;
}

.target-card__url {
  margin: 14px 0 0;
  color: #24344f;
  word-break: break-all;
}

.headers-preview {
  margin-top: 12px;
}

.headers-preview summary {
  cursor: pointer;
  color: #264f7d;
  font-weight: 700;
}

.primary-button {
  background: linear-gradient(135deg, #14213d 0%, #264f7d 100%);
  color: #fff;
}

.result {
  margin-top: 18px;
  padding: 16px;
  border-radius: 18px;
}

.result--success {
  background: #edfdf2;
  border: 1px solid #b5e4c5;
}

.result--error {
  background: #fff1f1;
  border: 1px solid #f1c0c0;
}

.result__headline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.result__message,
.result__meta {
  margin: 10px 0 0;
  color: #46546c;
}

.headers {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 18px;
}

pre {
  overflow-x: auto;
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: #18263f;
  color: #fff9ed;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .guide {
    padding: 20px;
  }

  .tester__toolbar,
  .target-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .headers {
    grid-template-columns: 1fr;
  }
}
</style>
