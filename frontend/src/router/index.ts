import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import BusinessApplyView from '../views/BusinessApplyView.vue'
import BusinessConfirmView from '../views/BusinessConfirmView.vue'
import HumanTransferView from '../views/HumanTransferView.vue'
import AiChatView from '../views/AiChatView.vue'
import RecordsView from '../views/RecordsView.vue'
import ProfileView from '../views/ProfileView.vue'
import ChannelAuthView from '../views/ChannelAuthView.vue'
import { hasValidAccessToken } from '../auth/session'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/channel-auth',
      name: 'channel-auth',
      component: ChannelAuthView
    },
    {
      path: '/business/:code',
      name: 'business-apply',
      component: BusinessApplyView,
      meta: { requiresAuth: true }
    },
    {
      path: '/business/:code/confirm',
      name: 'business-confirm',
      component: BusinessConfirmView,
      meta: { requiresAuth: true }
    },
    {
      path: '/business/:code/transfer',
      name: 'human-transfer',
      component: HumanTransferView,
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-chat',
      name: 'ai-chat',
      component: AiChatView,
      meta: { requiresAuth: true }
    },
    {
      path: '/records',
      name: 'records',
      component: RecordsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth || hasValidAccessToken()) {
    return true
  }
  return {
    name: 'channel-auth',
    query: {
      entryToken: String(to.query.entryToken || 'DEMO-ENTRY-001')
    }
  }
})

export default router
