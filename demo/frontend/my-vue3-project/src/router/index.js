import { createRouter, createWebHistory } from 'vue-router';

import 'element-plus/dist/index.css'
// 导入组件
import LoginPage from '../components/Login.vue';
import UserPage from '../components/UserPage.vue';
import PatientManage from '../components/PatientManage.vue';
import PatientPage from '../components/PatientPage.vue';  // 确保这里是正确的文件名
import AdminPage from '../components/AdminPage.vue';  // 确保这里是正确的文件名

const routes = [
  {
    path: '/',
    name: 'login',
    component: LoginPage,
  },
  {
    path: '/user',
    name: 'user',
    component: UserPage,
  },
  {
    path: '/patient-manage',
    name: 'PatientManage',
    component: PatientManage,
  },
  {
    path: '/patient',
    name: 'PatientPage',  // 确保这里的路由名称与组件名称一致
    component: PatientPage,
  },
  {
    path: '/admin',
    name: 'AdminPage',  // 确保这里的路由名称与组件名称一致
    component: AdminPage,
  },
  {
    path: '/bluetooth',
    name: 'BlueTooth',
    component: () => import('@/components/BlueTooth.vue')
  },
  {
    path: '/patient-manage',
    name: 'PatientManage',
    component: () => import('@/components/PatientManage.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
