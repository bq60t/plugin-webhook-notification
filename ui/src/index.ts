import { definePlugin } from '@halo-dev/ui-shared'
import { markRaw } from 'vue'
import HomeView from './views/HomeView.vue'

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {
    'plugin:self:tabs:create': () => [
      {
        id: 'webhook-notification-guide',
        label: 'Webhook 指南',
        component: markRaw(HomeView),
        permissions: [],
      },
    ],
  },
})
