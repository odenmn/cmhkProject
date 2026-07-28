import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import BusinessApplyView from '../views/BusinessApplyView.vue'
import BusinessConfirmView from '../views/BusinessConfirmView.vue'
import HumanTransferView from '../views/HumanTransferView.vue'
import AiChatView from '../views/AiChatView.vue'
import RecordsView from '../views/RecordsView.vue'
import ProfileView from '../views/ProfileView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/business/:code',
      name: 'business-apply',
      component: BusinessApplyView
    },
    {
      path: '/business/:code/confirm',
      name: 'business-confirm',
      component: BusinessConfirmView
    },
    {
      path: '/business/:code/transfer',
      name: 'human-transfer',
      component: HumanTransferView
    },
    {
      path: '/ai-chat',
      name: 'ai-chat',
      component: AiChatView
    },
    {
      path: '/records',
      name: 'records',
      component: RecordsView
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView
    }
  ]
})

export default router
